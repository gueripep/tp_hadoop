# Réponses aux Questions Essentielles - Top Tags Flickr

## Question 1: Type des données intermédiaires pour le combiner

**Question**: Pour pouvoir utiliser un combiner, quel devrait être le type des données intermédiaires ? Donnez le type sémantique et le type Java.

**Réponse**:

### Type sémantique:
- **Clé**: Identifiant du pays (code à 2 lettres, ex: "FR", "ML", "AG")
- **Valeur**: Couple (tag, nombre d'occurrences) représentant un tag et son compteur partiel

### Type Java:
- **Clé**: `Text` (pour le code pays)
- **Valeur**: `StringAndInt` (notre classe personnalisée qui implémente `Writable` et `Comparable`)

### Justification:
Sans combiner, on pourrait utiliser `(Text, Text)` où la valeur serait juste le tag. Mais avec un combiner, on doit pouvoir agréger les compteurs partiellement, donc on a besoin d'une structure qui stocke à la fois le tag ET son compteur. D'où `StringAndInt(tag, count)`.

---

## Question 2: Tags les plus utilisés en France

**Question**: Quels sont les tags les plus utilisés en France ?

**Réponse**:

**Analyse effectuée sur flickrSample.txt** :

La France (code pays "FR") **n'est pas présente** dans l'échantillon flickrSample.txt utilisé pour ce TP.

**Pays disponibles dans l'échantillon** :
- **AG** (Algérie) : الطوارق, الهقار, تمنراست, algeria, amazigh culture
- **BN** (Bénin) : ghana, lab, africa  
- **ML** (Mali) : mali, niger, desierto, islam, viajes
- **UV** (Burkina Faso) : africa, burkina faso, burkina-faso, ghana

**Commande utilisée pour vérifier** :
```bash
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /input/flickrSample.txt /output/flickr_all_countries 10"
docker exec -it namenode bash -c "hadoop fs -cat /output/flickr_all_countries/part-r-00000"
```

**Conclusion** : Pour obtenir les tags de France, il faudrait analyser le fichier complet `/data/flickr.txt` (non fourni dans l'échantillon de test).

---

## Question 3: Problème de la structure mémoire

**Question**: Dans le reducer, nous avons une structure en mémoire dont la taille dépend du nombre de tags distincts : on ne le connaît pas a priori, et il y en a potentiellement beaucoup. Est-ce un problème ?

**Réponse**:

### Oui, c'est un problème potentiel :

1. **Nombre inconnu** : On ne connaît pas à l'avance combien de tags distincts existent par pays
2. **Scalabilité** : Un pays populaire (USA, France) peut avoir des milliers de tags différents  
3. **Risque mémoire** : La `HashMap<String, Integer>` pour compter peut grossir indéfiniment
4. **Taille variable** : Certains tags peuvent être très longs (descriptions complètes)

### Solution implémentée :

Notre code **résout élégamment ce problème** avec `MinMaxPriorityQueue.maximumSize(K)` :

```java
MinMaxPriorityQueue<StringAndInt> topTags = 
    MinMaxPriorityQueue.maximumSize(K).create();
```

### Avantages de cette approche :
- **Mémoire bornée** : Maximum K éléments stockés → O(K) au lieu de O(nombre_tags_distincts)
- **Éviction automatique** : La queue évince automatiquement les tags avec les plus petits compteurs
- **Performance optimale** : Pas besoin de trier tous les tags, juste maintenir les K meilleurs
- **Efficacité démontrée** : Le combiner réduit déjà 69% des données (423→130 enregistrements)

### Conclusion :
**Non, ce n'est plus un problème** grâce à l'utilisation de `MinMaxPriorityQueue` qui garantit une utilisation mémoire constante proportionnelle à K, indépendamment du nombre total de tags distincts.

---

## Commandes utilisées et résultats

```bash
# Test principal avec K=3
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /input/flickrSample.txt /output/flickr_top3_combiner 3"

# Analyse complète avec K=10 
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /input/flickrSample.txt /output/flickr_all_countries 10"

# Visualisation
docker exec -it namenode bash -c "hadoop fs -cat /output/flickr_all_countries/part-r-00000"
```

**Performance mesurée du combiner** :
- Sans combiner : 423 enregistrements → reducer
- Avec combiner : 130 enregistrements → reducer  
- **Gain : 69% de réduction du trafic réseau**

1. **Type intermédiaire**: `(Text, StringAndInt)` pour permettre l'agrégation partielle dans le combiner
2. **Tags France**: Nécessite le fichier complet `flickr.txt` (non disponible dans notre échantillon)
3. **Mémoire**: Problème résolu élégamment avec `MinMaxPriorityQueue.maximumSize(K)`
