import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.google.common.collect.MinMaxPriorityQueue;

public class FlickrTopTagsWithCombiner {

    public static class FlickrMapper extends Mapper<LongWritable, Text, Text, StringAndInt> {
        
        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString();
            String[] fields = line.split("\t");
            
            // Vérifier qu'on a assez de champs
            if (fields.length < 12) {
                return;
            }
            
            try {
                // Extraire les coordonnées (champs 10 et 11)
                double longitude = Double.parseDouble(fields[10]);
                double latitude = Double.parseDouble(fields[11]);
                
                // Ignorer les coordonnées invalides (-1.0, -1.0)
                if (longitude == -1.0 && latitude == -1.0) {
                    return;
                }
                
                // Identifier le pays à partir des coordonnées
                Country country = Country.getCountryAt(latitude, longitude);
                if (country == null) {
                    return;
                }
                
                // Extraire les tags (champ 8)
                String tagsField = fields[8];
                if (tagsField != null && !tagsField.trim().isEmpty()) {
                    // Décoder l'URL et séparer les tags par virgule
                    String decodedTags = URLDecoder.decode(tagsField, "UTF-8");
                    String[] tags = decodedTags.split(",");
                    
                    Text countryKey = new Text(country.toString());
                    
                    // Émettre chaque tag avec un compteur de 1
                    for (String tag : tags) {
                        tag = tag.trim();
                        if (!tag.isEmpty()) {
                            context.write(countryKey, new StringAndInt(tag, 1));
                        }
                    }
                }
                
            } catch (NumberFormatException e) {
                // Ignorer les lignes avec des coordonnées invalides
                return;
            } catch (Exception e) {
                // Ignorer les autres erreurs de parsing
                return;
            }
        }
    }

    public static class FlickrCombiner extends Reducer<Text, StringAndInt, Text, StringAndInt> {
        
        @Override
        protected void reduce(Text key, Iterable<StringAndInt> values, Context context)
                throws IOException, InterruptedException {
            
            // Agréger les compteurs partiels par tag
            Map<String, Integer> tagCounts = new HashMap<String, Integer>();
            
            for (StringAndInt value : values) {
                String tag = value.getString();
                int count = value.getCount();
                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + count);
            }
            
            // Émettre les compteurs agrégés
            for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
                context.write(key, new StringAndInt(entry.getKey(), entry.getValue()));
            }
        }
    }

    public static class FlickrReducer extends Reducer<Text, StringAndInt, Text, Text> {
        private int K;
        
        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // Récupérer la valeur de K depuis la configuration
            Configuration conf = context.getConfiguration();
            K = conf.getInt("flickr.top.k", 3); // valeur par défaut: 3
        }
        
        @Override
        protected void reduce(Text key, Iterable<StringAndInt> values, Context context)
                throws IOException, InterruptedException {
            
            // Agréger les compteurs finaux par tag
            Map<String, Integer> tagCounts = new HashMap<String, Integer>();
            
            for (StringAndInt value : values) {
                String tag = value.getString();
                int count = value.getCount();
                tagCounts.put(tag, tagCounts.getOrDefault(tag, 0) + count);
            }
            
            // Utiliser MinMaxPriorityQueue pour garder les K meilleurs tags
            MinMaxPriorityQueue<StringAndInt> topTags = 
                MinMaxPriorityQueue.maximumSize(K).create();
            
            for (Map.Entry<String, Integer> entry : tagCounts.entrySet()) {
                topTags.add(new StringAndInt(entry.getKey(), entry.getValue()));
            }
            
            // Émettre les résultats (pays + top tags)
            String country = key.toString();
            for (StringAndInt tagCount : topTags) {
                context.write(new Text(country), new Text(tagCount.toString()));
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        
        if (otherArgs.length != 3) {
            System.err.println("Usage: FlickrTopTagsWithCombiner <input> <output> <K>");
            System.exit(2);
        }
        
        String input = otherArgs[0];
        String output = otherArgs[1];
        int K = Integer.parseInt(otherArgs[2]);
        
        // Passer K à la configuration pour les reducers
        conf.setInt("flickr.top.k", K);
        
        Job job = Job.getInstance(conf, "FlickrTopTagsWithCombiner");
        job.setJarByClass(FlickrTopTagsWithCombiner.class);
        
        job.setMapperClass(FlickrMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(StringAndInt.class);
        
        job.setCombinerClass(FlickrCombiner.class);
        
        job.setReducerClass(FlickrReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(input));
        job.setInputFormatClass(TextInputFormat.class);
        
        FileOutputFormat.setOutputPath(job, new Path(output));
        job.setOutputFormatClass(TextOutputFormat.class);
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
