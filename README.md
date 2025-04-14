# Système de Stéganographie avec Codage de Huffman

Ce projet implémente un système de stéganographie qui permet de cacher des messages dans des fichiers PNG et WAV en utilisant la technique du bit de poids faible (LSB - Least Significant Bit) combinée avec le codage de Huffman pour la compression du message.

## Fonctionnalités

- Masquage et extraction de messages dans des images PNG
- Masquage et extraction de messages dans des fichiers audio WAV
- Compression des messages avec le codage de Huffman
- Stratégie de positionnement séquentiel pour garantir la fiabilité
- Système de fallback en cas d'échec de décodage (extraction ASCII)

## Comment ça marche

### Préparation des données

1. **Compression du message** : Le message est d'abord compressé en utilisant le codage de Huffman, qui attribue des codes binaires plus courts aux caractères les plus fréquents.
2. **Sérialisation** : Le dictionnaire de fréquences et le message codé sont ensuite sérialisés dans une chaîne binaire selon un format spécifique.
3. **Conversion en octets** : Cette chaîne binaire est convertie en un tableau d'octets pour faciliter le masquage.

### Stéganographie pour les images PNG

- **Masquage** : Le programme cache les bits du message dans les bits de poids faible (LSB) des composantes de couleur des pixels de l'image, en suivant un parcours séquentiel.
- **Extraction** : Pour extraire le message, le programme lit les LSB des pixels dans le même ordre séquentiel, reconstruit le message binaire, puis le décode en utilisant l'arbre de Huffman.

### Stéganographie pour les fichiers WAV

- **Masquage** : De façon similaire, les bits du message sont cachés dans les LSB des échantillons audio, en tenant compte de la structure du fichier WAV.
- **Extraction** : L'extraction suit le même processus séquentiel pour récupérer les bits, puis reconstruire et décoder le message original.

## Structure du projet

- `src/App.java` : Classe principale pour tester le système
- `src/CodageHuffman.java` : Implémentation du codage de Huffman
- `src/NoeudHuffman.java` : Structure de données pour l'arbre de Huffman
- `src/SteganographieSimple.java` : Classe pour la stéganographie dans les images PNG
- `src/SteganographieWAVSimple.java` : Classe pour la stéganographie dans les fichiers WAV

## Comment utiliser

1. Préparez un fichier PNG nommé `input.png` dans le répertoire `images/`
2. Préparez un fichier WAV nommé `input.wav` dans le répertoire `audio/`
3. Exécutez le programme principal (`App.java`)
4. Le programme cachera le message dans les fichiers et tentera de l'extraire
5. Les fichiers résultants seront `simple_output.png` et `simple_output.wav`

## Points forts de cette implémentation

1. **Simplicité et robustesse** : Approche séquentielle directe sans positionnement aléatoire qui pourrait causer des incohérences.
2. **Format de données clair** : Structure de sérialisation bien définie avec vérifications de validité.
3. **Mécanismes de secours** : Si le décodage Huffman échoue, le système tente une extraction ASCII directe.
4. **Diagnostics détaillés** : Messages de débogage à chaque étape pour faciliter le dépannage.

## Limitations

- La technique LSB ne résiste pas aux modifications d'image (compression, recadrage, etc.)
- La capacité dépend de la taille du support (fichier PNG ou WAV)
- Les fichiers WAV doivent être en format PCM non compressé
