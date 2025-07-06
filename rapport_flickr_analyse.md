# Rapport d'Analyse - Top Tags Flickr par Pays

## 3. Top-tags Flickr par pays, avec tri en mémoire

### 3.1 Implémentation Map et Reduce

Le programme `FlickrTopTags` analyse les métadonnées de photos Flickr pour identifier les tags les plus utilisés par pays.

#### Architecture:
- **Mapper**: Extrait les coordonnées GPS, identifie le pays avec la classe `Country`, décode les tags URL et émet `(pays, tag)`
- **Reducer**: Compte les occurrences de chaque tag par pays, utilise `MinMaxPriorityQueue` pour garder les K meilleurs tags

#### Classes créées:
- `StringAndInt.java`: Implémente `Comparable<StringAndInt>` et `Writable` pour encapsuler un tag et son nombre d'occurrences
- `Country.java`: Classe fournie pour géolocalisation à partir des coordonnées GPS

### 3.2 Combiner

#### Question: Type des données intermédiaires pour le combiner
**Type sémantique**: Clé = pays (String), Valeur = (tag, count) 
**Type Java**: `Text` pour la clé, `StringAndInt` pour la valeur

Le combiner permet d'agréger localement les comptes de tags avant transmission au reducer, réduisant significativement le trafic réseau.

#### Implémentation:
- `FlickrTopTagsWithCombiner.java`: Version avec combiner
- **Mapper**: Émet `(pays, StringAndInt(tag, 1))`
- **Combiner**: Classe `FlickrCombiner` qui agrège les comptes par tag localement
- **Reducer**: Traite les comptes pré-agrégés et applique le top-K final

### 3.3 Résultats des Tests

#### Test sur flickrSample.txt avec K=3:

**Résultats identiques avec et sans combiner:**

```
AG  الطوارق    3    (Touaregs en arabe)
AG  الهقار     3    (Hoggar en arabe) 
AG  تمنراست   3    (Tamanrasset en arabe)
BN  ghana      7
BN  lab        5
BN  africa     2
ML  mali       15
ML  niger      11
ML  desierto   10
UV  africa     10
UV  burkina faso  9
UV  burkina-faso 9
```

#### Test avec K=5:

```
AG  الطوارق         3
AG  الهقار          3  
AG  تمنراست        3
AG  algeria         3
AG  amazigh culture 3
BN  ghana           7
BN  idds            2
BN  single mothers  1
BN  lab             5
BN  africa          2
ML  mali            15
ML  rio niger       10
ML  viajes          10
ML  niger           11
ML  islam           10
UV  africa          10
UV  ghana           8
UV  burkina faso    9
UV  burkina-faso    9
UV  afrique         9
```

### 3.4 Analyse des Performances

#### Efficacité du Combiner:

**Sans combiner:**
- Map output records: 423
- Reduce input records: 423
- Spilled Records: 846

**Avec combiner:**
- Map output records: 423
- Combine input records: 423
- Combine output records: 130
- Reduce input records: 130
- Spilled Records: 260

**Gain du combiner**: Réduction de ~69% des données transmises (423 → 130 enregistrements)

### 3.5 Questions Techniques

#### Question: Gestion mémoire dans le reducer
La structure `HashMap<String, Integer>` pour compter les tags peut devenir problématique si:
- Le nombre de tags distincts par pays est très élevé
- Les tags sont très longs (noms de lieux en langues locales)

**Solution**: Utilisation de `MinMaxPriorityQueue.maximumSize(K)` qui limite automatiquement la mémoire en ne gardant que les K meilleurs tags.

### 3.6 Commandes Utilisées

```bash
# Compilation et exécution sans combiner
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTags.jar FlickrTopTags /input/flickrSample.txt /output/flickr_top3 3"

# Compilation et exécution avec combiner  
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /input/flickrSample.txt /output/flickr_top3_combiner 3"

# Test avec K=5
docker exec -it namenode bash -c "hadoop jar /job/FlickrTopTagsWithCombiner.jar FlickrTopTagsWithCombiner /input/flickrSample.txt /output/flickr_top5 5"

# Visualisation des résultats
docker exec -it namenode bash -c "hadoop fs -cat /output/flickr_top3/part-r-00000"
```

### 3.7 Observations

1. **Géolocalisation**: Les codes pays AG (Algérie), ML (Mali), UV (Burkina Faso), BN montrent une bonne couverture de l'Afrique de l'Ouest
2. **Tags multilingues**: Présence de tags en arabe pour l'Algérie, reflétant la diversité linguistique
3. **Cohérence géographique**: Les tags "niger", "mali", "burkina faso" apparaissent logiquement dans les pays voisins
4. **Efficacité du combiner**: Réduction significative du trafic réseau sans impact sur les résultats finaux

Le programme fonctionne correctement et le combiner apporte un gain de performance substantiel.
