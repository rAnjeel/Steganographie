# Stéganographie avec Codage Huffman

Ce projet implémente un système de stéganographie permettant de cacher des messages (des versets bibliques) dans des fichiers PNG ou WAV en utilisant le codage de Huffman.

## Principe de fonctionnement

1. **Prénom et Numéro** : Le prénom de l'utilisateur et un numéro sont utilisés comme paramètres pour générer aléatoirement les emplacements des bits du message dans le fichier.
2. **Codage de Huffman** : Le message est d'abord compressé en utilisant l'algorithme de Huffman avant d'être caché.
3. **Stéganographie** : Le message codé est ensuite dissimulé dans les bits les moins significatifs des fichiers PNG (dans les pixels) ou WAV (dans les échantillons audio).

## Structure du projet

- `src/CodageHuffman.java` : Implémentation de l'algorithme de Huffman pour compresser et décompresser le message.
- `src/NoeudHuffman.java` : Classe représentant les nœuds de l'arbre de Huffman.
- `src/SteganographiePNG.java` : Gestion de la stéganographie dans les fichiers PNG.
- `src/SteganographieWAV.java` : Gestion de la stéganographie dans les fichiers WAV.
- `src/App.java` : Classe principale démontrant l'utilisation du système.
- `images/` : Répertoire pour les fichiers PNG d'entrée et de sortie.
- `audio/` : Répertoire pour les fichiers WAV d'entrée et de sortie.

## Utilisation

1. Placez vos fichiers PNG ou WAV dans les répertoires `images/` et `audio/` respectivement avec le nom `input.png` ou `input.wav`.
2. Modifiez le verset biblique, le prénom et le numéro dans la classe `App.java` si nécessaire.
3. Exécutez l'application :
   ```
   javac src/*.java
   java -cp src App
   ```
4. Les fichiers contenant les messages cachés seront générés sous les noms `output.png` et `output.wav` dans leurs répertoires respectifs.

## Sécurité

La sécurité du système repose sur :

1. Le prénom et le numéro utilisés comme graines pour la génération des positions aléatoires.
2. L'algorithme de Huffman qui transforme le texte original en séquence binaire.
3. La méthode de dissimulation des bits dans les fichiers PNG ou WAV.

## Exigences

- Java 8 ou supérieur
- Les bibliothèques standard de Java pour la manipulation des images et du son
