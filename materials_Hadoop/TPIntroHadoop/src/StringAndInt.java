import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;

public class StringAndInt implements Comparable<StringAndInt>, Writable {
    private Text string;
    private int count;
    
    // Constructeur sans arguments (requis pour Writable)
    public StringAndInt() {
        this.string = new Text();
        this.count = 0;
    }
    
    // Constructeur avec paramètres
    public StringAndInt(String string, int count) {
        this.string = new Text(string);
        this.count = count;
    }
    
    // Getters
    public String getString() {
        return string.toString();
    }
    
    public int getCount() {
        return count;
    }
    
    // Setters
    public void setString(String string) {
        this.string.set(string);
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    // Compare par nombre d'occurrences (ordre décroissant pour top-K)
    @Override
    public int compareTo(StringAndInt other) {
        // Ordre décroissant : other.count - this.count
        return Integer.compare(other.count, this.count);
    }
    
    // Implémentation de Writable pour sérialisation
    @Override
    public void write(DataOutput out) throws IOException {
        string.write(out);
        out.writeInt(count);
    }
    
    @Override
    public void readFields(DataInput in) throws IOException {
        string.readFields(in);
        count = in.readInt();
    }
    
    @Override
    public String toString() {
        return string.toString() + "\t" + count;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StringAndInt that = (StringAndInt) obj;
        return count == that.count && string.equals(that.string);
    }
    
    @Override
    public int hashCode() {
        return string.hashCode() * 31 + count;
    }
}
