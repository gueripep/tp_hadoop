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

Pour répondre à cette question, il faudrait exécuter le programme sur le fichier complet `/data/flickr.txt` et filtrer les résultats pour le code pays "FR".

**Commande à exécuter**:
```bash
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /data/flickr.txt /output/flickr_france 5"
docker exec -it namenode bash -c "hadoop fs -cat /output/flickr_france/part-r-00000 | grep '^FR'"
```

**Limitation actuelle**: 
Le fichier `flickrSample.txt` que nous avons testé ne contient pas de photos prises en France (codes pays présents: AG, BN, ML, UV). Pour obtenir les tags français, il faut le fichier complet `flickr.txt` mentionné dans l'énoncé.

**Résultats attendus**: Probablement des tags comme "paris", "france", "tour eiffel", "louvre", etc.

---

## Question 3: Problème de la structure mémoire

**Question**: Dans le reducer, nous avons une structure en mémoire dont la taille dépend du nombre de tags distincts : on ne le connaît pas a priori, et il y en a potentiellement beaucoup. Est-ce un problème ?

**Réponse**:

### Oui, c'est un problème potentiel:

1. **Nombre inconnu**: On ne connaît pas à l'avance combien de tags distincts existent par pays
2. **Croissance imprévisible**: Un pays populaire peut avoir des milliers de tags différents
3. **Risque de OutOfMemoryError**: La `HashMap<String, Integer>` peut grossir indéfiniment

### Solution implémentée:

Notre code résout ce problème avec `MinMaxPriorityQueue.maximumSize(K)`:

```java
MinMaxPriorityQueue<StringAndInt> topTags = 
    MinMaxPriorityQueue.maximumSize(K).create();
```

### Avantages de cette approche:
- **Mémoire bornée**: Maximum K éléments stockés (O(K) au lieu de O(nombre_tags_distincts))
- **Éviction automatique**: La queue évince automatiquement les tags avec les plus petits compteurs
- **Performance optimale**: Pas besoin de trier tous les tags, juste maintenir les K meilleurs

### Conclusion:
**Non, ce n'est plus un problème** grâce à l'utilisation de `MinMaxPriorityQueue` qui garantit une utilisation mémoire constante proportionnelle à K, indépendamment du nombre total de tags distincts.

---

## Résumé

1. **Type intermédiaire**: `(Text, StringAndInt)` pour permettre l'agrégation partielle dans le combiner
2. **Tags France**: Nécessite le fichier complet `flickr.txt` (non disponible dans notre échantillon)
3. **Mémoire**: Problème résolu élégamment avec `MinMaxPriorityQueue.maximumSize(K)`
