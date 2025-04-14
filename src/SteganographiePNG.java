import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import javax.imageio.ImageIO;

public class SteganographiePNG {
    
    // Cache le message dans l'image
    public static void cacherMessage(String cheminImage, String cheminSortie, String message, String prenom, int numero) throws IOException {
        // Convertir le message en chaîne binaire à l'aide du codage Huffman
        Map<Character, Integer> frequences = new HashMap<>();
        for (char c : message.toCharArray()) {
            frequences.put(c, frequences.getOrDefault(c, 0) + 1);
        }
        
        NoeudHuffman racineHuffman = CodageHuffman.construireArbreHuffman(frequences);
        CodageHuffman.genererCodes(racineHuffman, "");
        String messageBinaire = CodageHuffman.encoder(message);
        
        // Ajouter la longueur du message binaire et la table de fréquences au début
        String donneesCompletes = serializerDonnees(messageBinaire, frequences);
        int totalBits = donneesCompletes.length();
        
        System.out.println("Longueur totale à cacher: " + totalBits + " bits");
        System.out.println("Message binaire Huffman: " + messageBinaire);
        
        // Lire l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        System.out.println("Dimensions de l'image: " + largeur + "x" + hauteur);
        
        // Vérifier si l'image est assez grande pour contenir le message
        if (totalBits > largeur * hauteur * 3) {
            throw new IOException("L'image est trop petite pour contenir le message (" + 
                totalBits + " bits nécessaires, " + (largeur * hauteur * 3) + " bits disponibles).");
        }
        
        // Approche 1: Méthode séquentielle (cacher les bits dans l'ordre)
        // Cette méthode est plus simple et correspond à la nouvelle méthode d'extraction
        int compteur = 0;
        
        outerLoop:
        for (int y = 0; y < hauteur && compteur < totalBits; y++) {
            for (int x = 0; x < largeur && compteur < totalBits; x++) {
                // Obtenir la couleur RGB du pixel
                int pixel = image.getRGB(x, y);
                
                int rouge = (pixel >> 16) & 0xff;
                int vert = (pixel >> 8) & 0xff;
                int bleu = pixel & 0xff;
                int alpha = (pixel >> 24) & 0xff;
                
                // Modifier le bit LSB des trois composantes RGB si possible
                if (compteur < totalBits) {
                    char bit = donneesCompletes.charAt(compteur);
                    int valeurBit = (bit == '1') ? 1 : 0;
                    rouge = (rouge & 0xFE) | valeurBit;
                    compteur++;
                }
                
                if (compteur < totalBits) {
                    char bit = donneesCompletes.charAt(compteur);
                    int valeurBit = (bit == '1') ? 1 : 0;
                    vert = (vert & 0xFE) | valeurBit;
                    compteur++;
                }
                
                if (compteur < totalBits) {
                    char bit = donneesCompletes.charAt(compteur);
                    int valeurBit = (bit == '1') ? 1 : 0;
                    bleu = (bleu & 0xFE) | valeurBit;
                    compteur++;
                }
                
                // Reconstituer le pixel
                int nouveauPixel = (alpha << 24) | (rouge << 16) | (vert << 8) | bleu;
                image.setRGB(x, y, nouveauPixel);
                
                if (compteur >= totalBits) break outerLoop;
            }
        }
        
        // Enregistrer l'image modifiée
        ImageIO.write(image, "png", new File(cheminSortie));
        
        System.out.println("Message caché avec succès. Bits utilisés: " + compteur + " sur " + 
            (largeur * hauteur * 3) + " disponibles (méthode séquentielle).");
    }
    
    // Extrait le message caché de l'image
    public static String extraireMessage(String cheminImage, String prenom, int numero) throws IOException, ClassNotFoundException {
        // Lire l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        System.out.println("Dimensions de l'image: " + largeur + "x" + hauteur);
        
        // Approche simplifiée: extraction directe bit par bit
        // 1. Extraire les 32 premiers bits pour déterminer la longueur
        StringBuilder longueurBinaire = new StringBuilder();
        int compteur = 0;
        
        // Parcourir tous les pixels de l'image dans l'ordre
        for (int y = 0; y < hauteur && compteur < 32; y++) {
            for (int x = 0; x < largeur && compteur < 32; x++) {
                int pixel = image.getRGB(x, y);
                
                // Extraire les bits LSB des composantes RGB
                int rougeValue = (pixel >> 16) & 1;
                int vertValue = (pixel >> 8) & 1;
                int bleuValue = pixel & 1;
                
                longueurBinaire.append(rougeValue);
                compteur++;
                if (compteur >= 32) break;
                
                longueurBinaire.append(vertValue);
                compteur++;
                if (compteur >= 32) break;
                
                longueurBinaire.append(bleuValue);
                compteur++;
                if (compteur >= 32) break;
            }
        }
        
        try {
            // Convertir la chaîne binaire en un entier
            String longueurStr = longueurBinaire.toString();
            System.out.println("Longueur binaire extraite: " + longueurStr);
            
            int longueurTotale = Integer.parseInt(longueurStr, 2);
            System.out.println("Longueur totale décodée: " + longueurTotale);
            
            // Vérification de sécurité sur la longueur
            if (longueurTotale <= 0 || longueurTotale > largeur * hauteur * 3) {
                throw new IllegalArgumentException("Longueur invalide: " + longueurTotale);
            }
            
            // Extraire toutes les données binaires (y compris les 32 premiers bits)
            StringBuilder donneesBinaires = new StringBuilder(longueurBinaire);
            compteur = 32; // Commencer après les 32 bits de longueur
            
            // Continuer à parcourir l'image pour extraire le reste des bits
            outerLoop:
            for (int y = 0; y < hauteur && compteur < longueurTotale; y++) {
                for (int x = 0; x < largeur && compteur < longueurTotale; x++) {
                    // Pour les 32 premiers bits, continuer depuis où on s'est arrêté
                    if (y == 0 && x < 11) { // 32 / 3 ≈ 11 pixels ont déjà été lus
                        int bitsLus = (x * 3) + (y * largeur * 3);
                        if (bitsLus < 32) continue; // Sauter les pixels déjà lus
                    }
                    
                    int pixel = image.getRGB(x, y);
                    
                    // Extraire les bits LSB des composantes RGB
                    int rougeValue = (pixel >> 16) & 1;
                    donneesBinaires.append(rougeValue);
                    compteur++;
                    if (compteur >= longueurTotale) break outerLoop;
                    
                    int vertValue = (pixel >> 8) & 1;
                    donneesBinaires.append(vertValue);
                    compteur++;
                    if (compteur >= longueurTotale) break outerLoop;
                    
                    int bleuValue = pixel & 1;
                    donneesBinaires.append(bleuValue);
                    compteur++;
                    if (compteur >= longueurTotale) break outerLoop;
                }
            }
            
            System.out.println("Bits extraits: " + compteur + "/" + longueurTotale);
            
            // Méthode alternative: utiliser la méthode d'extraction originale
            if (compteur < longueurTotale) {
                System.out.println("Tentative d'extraction avec la méthode basée sur les positions aléatoires...");
                return extraireMessageOriginal(image, prenom, numero);
            }
            
            // Désérialiser les données
            return deserializerDonnees(donneesBinaires.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Erreur de décodage: " + e.getMessage());
        }
    }
    
    // Méthode d'extraction originale basée sur les positions générées aléatoirement
    private static String extraireMessageOriginal(BufferedImage image, String prenom, int numero) throws IOException {
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Carte pour stocker toutes les positions à utiliser dans l'ordre
        int[][] positions = new int[largeur * hauteur * 3][3]; // [x, y, composante]
        int posCount = 0;
        boolean[] posOccupees = new boolean[largeur * hauteur * 3];
        
        // Générer toutes les positions possibles à l'avance 
        // Augmenter la limite pour s'assurer d'avoir assez de positions
        while (posCount < largeur * hauteur * 3 && posCount < 1000000) {
            int x = random.nextInt(largeur);
            int y = random.nextInt(hauteur);
            int position = (y * largeur + x);
            int composante = random.nextInt(3);
            int index = position * 3 + composante;
            
            if (!posOccupees[index]) {
                positions[posCount][0] = x;
                positions[posCount][1] = y;
                positions[posCount][2] = composante;
                posOccupees[index] = true;
                posCount++;
            }
        }
        
        System.out.println("Positions générées: " + posCount);
        
        // Extraire les 32 premiers bits pour la longueur
        StringBuilder longueurBinaire = new StringBuilder();
        for (int i = 0; i < 32 && i < posCount; i++) {
            int x = positions[i][0];
            int y = positions[i][1];
            int composante = positions[i][2];
            
            int pixel = image.getRGB(x, y);
            int valeur = 0;
            
            switch (composante) {
                case 0: // Rouge
                    valeur = (pixel >> 16) & 1;
                    break;
                case 1: // Vert
                    valeur = (pixel >> 8) & 1;
                    break;
                case 2: // Bleu
                    valeur = pixel & 1;
                    break;
            }
            
            longueurBinaire.append(valeur);
        }
        
        try {
            // Convertir la chaîne binaire en un entier
            String longueurStr = longueurBinaire.toString();
            System.out.println("Longueur binaire extraite (méthode originale): " + longueurStr);
            
            int longueurTotale = Integer.parseInt(longueurStr, 2);
            System.out.println("Longueur totale décodée (méthode originale): " + longueurTotale);
            
            // Extraire le reste des données (important: extraire au moins jusqu'à longueurTotale)
            StringBuilder donneesBinaires = new StringBuilder(longueurBinaire);
            
            // Extraire le reste des bits (on commence à l'indice 32)
            // Utiliser une plus grande limite pour posCount pour s'assurer d'extraire assez de bits
            for (int i = 32; i < Math.min(longueurTotale, posCount); i++) {
                int x = positions[i][0];
                int y = positions[i][1];
                int composante = positions[i][2];
                
                int pixel = image.getRGB(x, y);
                int valeur = 0;
                
                switch (composante) {
                    case 0: // Rouge
                        valeur = (pixel >> 16) & 1;
                        break;
                    case 1: // Vert
                        valeur = (pixel >> 8) & 1;
                        break;
                    case 2: // Bleu
                        valeur = pixel & 1;
                        break;
                }
                
                donneesBinaires.append(valeur);
            }
            
            System.out.println("Données extraites (méthode originale): " + donneesBinaires.length() + " bits");
            
            // Désérialiser les données
            return deserializerDonnees(donneesBinaires.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("Erreur d'extraction originale: " + e.getMessage());
        }
    }
    
    // Initialiser le générateur de nombres aléatoires en fonction du prénom et du numéro
    private static Random initialiserRandom(String prenom, int numero) {
        long seed = 0;
        for (char c : prenom.toCharArray()) {
            seed = 31 * seed + c;
        }
        seed = seed * 31 + numero;
        return new Random(seed);
    }
    
    // Sérialise les données (messageBinaire + fréquences) en chaîne binaire
    private static String serializerDonnees(String messageBinaire, Map<Character, Integer> frequences) {
        StringBuilder resultat = new StringBuilder();
        
        try {
            // Ajouter la longueur du message original (en caractères) sur 16 bits
            String longueurMsgOriginal = String.format("%16s", Integer.toBinaryString(frequences.values().stream().mapToInt(Integer::intValue).sum())).replace(' ', '0');
            resultat.append(longueurMsgOriginal);
            
            // Nombre de caractères uniques sur 8 bits
            String nbCaracteres = String.format("%8s", Integer.toBinaryString(frequences.size())).replace(' ', '0');
            resultat.append(nbCaracteres);
            
            // Pour chaque caractère: valeur ASCII (8 bits) + fréquence (8 bits)
            for (Map.Entry<Character, Integer> entry : frequences.entrySet()) {
                // Caractère (8 bits)
                char c = entry.getKey();
                String caractere = String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0');
                resultat.append(caractere);
                
                // Fréquence (8 bits, limitée à 255)
                int freq = Math.min(entry.getValue(), 255);
                String frequence = String.format("%8s", Integer.toBinaryString(freq)).replace(' ', '0');
                resultat.append(frequence);
            }
            
            // Ajouter le message binaire
            resultat.append(messageBinaire);
            
            // Calculer la longueur totale des données sans l'en-tête (les 32 premiers bits)
            int longueurDonnees = resultat.length();
            String longueurTotaleBin = String.format("%32s", Integer.toBinaryString(longueurDonnees)).replace(' ', '0');
            
            System.out.println("Longueur du résultat avant ajout de l'en-tête: " + longueurDonnees);
            
            // Retourner la longueur totale suivie des données
            return longueurTotaleBin + resultat.toString();
        } catch (Exception e) {
            System.err.println("Erreur lors de la sérialisation: " + e.getMessage());
            // En cas d'erreur, retourner simplement le message original encodé en ASCII
            StringBuilder msgAscii = new StringBuilder();
            for (char c : messageBinaire.toCharArray()) {
                msgAscii.append(String.format("%8s", Integer.toBinaryString(c)).replace(' ', '0'));
            }
            return String.format("%32s", Integer.toBinaryString(msgAscii.length())).replace(' ', '0') + msgAscii.toString();
        }
    }
    
    // Désérialise la chaîne binaire en message
    private static String deserializerDonnees(String donneesCompletes) {
        try {
            System.out.println("Longueur des données complètes: " + donneesCompletes.length());
            
            // Vérifier si la chaîne est assez longue
            if (donneesCompletes.length() < 32) {
                throw new IllegalArgumentException("Données trop courtes");
            }
            
            // Extraire la longueur des données (sans les 32 bits d'en-tête)
            int longueurDonnees = Integer.parseInt(donneesCompletes.substring(0, 32), 2);
            System.out.println("Longueur des données indiquée dans l'en-tête: " + longueurDonnees);
            
            // Si les données disponibles sont plus courtes que prévu, ajuster
            if (donneesCompletes.length() - 32 < longueurDonnees) {
                System.out.println("ATTENTION: Les données extraites sont plus courtes que prévu!");
                longueurDonnees = donneesCompletes.length() - 32;
            }
            
            // Extraire les données (sans les 32 bits d'en-tête)
            String donnees = donneesCompletes.substring(32, 32 + longueurDonnees);
            
            // Ne pas utiliser de formatage complexe, retourner directement le message
            if (donnees.length() < 32) {
                // Si les données sont trop courtes, tenter une extraction directe du message
                System.out.println("Données trop courtes, tentative d'extraction directe...");
                return extraireMessageDirect(donnees);
            }
            
            // Longueur du message original en caractères (16 bits)
            int longueurMessage = Integer.parseInt(donnees.substring(0, 16), 2);
            System.out.println("Longueur du message original: " + longueurMessage + " caractères");
            
            // Nombre de caractères uniques (8 bits)
            int nbCaracteres = Integer.parseInt(donnees.substring(16, 24), 2);
            System.out.println("Nombre de caractères uniques: " + nbCaracteres);
            
            // Si le nombre de caractères semble absurde, tenter une extraction directe
            if (nbCaracteres <= 0 || nbCaracteres > 128) {
                System.out.println("Nombre de caractères absurde, tentative d'extraction directe...");
                return extraireMessageDirect(donnees);
            }
            
            int position = 24;
            
            // Reconstruire la table de fréquences
            Map<Character, Integer> frequences = new HashMap<>();
            for (int i = 0; i < nbCaracteres && position + 16 <= donnees.length(); i++) {
                // Extraire le caractère (8 bits)
                int codeCaractere = Integer.parseInt(donnees.substring(position, position + 8), 2);
                position += 8;
                
                // Extraire la fréquence (8 bits)
                int frequence = Integer.parseInt(donnees.substring(position, position + 8), 2);
                position += 8;
                
                frequences.put((char)codeCaractere, frequence);
            }
            
            // Si la table est vide, c'est probablement une erreur
            if (frequences.isEmpty()) {
                System.out.println("Table de fréquences vide, tentative d'extraction directe...");
                return extraireMessageDirect(donnees);
            }
            
            System.out.println("Table de fréquences reconstruite: " + frequences);
            
            // Vérifier si la position actuelle est dans les limites
            if (position >= donnees.length()) {
                System.out.println("Position au-delà des limites, tentative d'extraction directe...");
                return extraireMessageDirect(donnees);
            }
            
            // Extraire le message binaire
            String messageBinaire = donnees.substring(position);
            
            // Reconstruire l'arbre de Huffman
            NoeudHuffman racineHuffman = CodageHuffman.construireArbreHuffman(frequences);
            CodageHuffman.genererCodes(racineHuffman, "");
            
            // Afficher la table de codage pour déboguer
            CodageHuffman.afficherCodes();
            
            // Décoder le message
            String resultat = CodageHuffman.decoder(messageBinaire);
            
            // Si le résultat est vide ou semble corrompu, tenter l'extraction directe
            if (resultat == null || resultat.isEmpty() || resultat.length() > longueurMessage * 2) {
                System.out.println("Résultat suspect, tentative d'extraction directe...");
                return extraireMessageDirect(donnees);
            }
            
            return resultat;
        } catch (Exception e) {
            System.err.println("Erreur de désérialisation: " + e.getMessage());
            e.printStackTrace();
            
            // En cas d'erreur, tenter une extraction directe
            return extraireMessageDirect(donneesCompletes.substring(Math.min(32, donneesCompletes.length())));
        }
    }

    // Méthode de secours pour extraire directement le message en cas d'échec
    private static String extraireMessageDirect(String donnees) {
        try {
            StringBuilder message = new StringBuilder();
            
            // Tenter d'interpréter les données comme une séquence de caractères ASCII (8 bits par caractère)
            for (int i = 0; i < donnees.length(); i += 8) {
                if (i + 8 <= donnees.length()) {
                    int charCode = Integer.parseInt(donnees.substring(i, i + 8), 2);
                    // Ne garder que les caractères imprimables ASCII
                    if (charCode >= 32 && charCode <= 126) {
                        message.append((char)charCode);
                    }
                }
            }
            
            // Si le message est trop long, le tronquer
            if (message.length() > 100) {
                return message.substring(0, 100);
            }
            
            return message.toString();
        } catch (Exception e) {
            return "Échec de l'extraction directe: " + e.getMessage();
        }
    }

    // Ancienne méthode avec positionnement aléatoire
    public static void cacherMessageAleatoire(String cheminImage, String cheminSortie, String message, String prenom, int numero) throws IOException {
        // Convertir le message en chaîne binaire à l'aide du codage Huffman
        Map<Character, Integer> frequences = new HashMap<>();
        for (char c : message.toCharArray()) {
            frequences.put(c, frequences.getOrDefault(c, 0) + 1);
        }
        
        NoeudHuffman racineHuffman = CodageHuffman.construireArbreHuffman(frequences);
        CodageHuffman.genererCodes(racineHuffman, "");
        String messageBinaire = CodageHuffman.encoder(message);
        
        // Ajouter la longueur du message binaire et la table de fréquences au début
        String donneesCompletes = serializerDonnees(messageBinaire, frequences);
        int totalBits = donneesCompletes.length();
        
        System.out.println("Longueur totale à cacher: " + totalBits + " bits");
        
        // Lire l'image
        BufferedImage image = ImageIO.read(new File(cheminImage));
        int largeur = image.getWidth();
        int hauteur = image.getHeight();
        
        // Vérifier si l'image est assez grande pour contenir le message
        if (totalBits > largeur * hauteur * 3) {
            throw new IOException("L'image est trop petite pour contenir le message.");
        }
        
        // Générer un générateur de nombres aléatoires basé sur le prénom et le numéro
        Random random = initialiserRandom(prenom, numero);
        
        // Générer toutes les positions possibles à l'avance
        int[][] positions = new int[totalBits][3]; // [x, y, composante]
        int posCount = 0;
        boolean[] posOccupees = new boolean[largeur * hauteur * 3];
        
        while (posCount < totalBits) {
            int x = random.nextInt(largeur);
            int y = random.nextInt(hauteur);
            int position = (y * largeur + x);
            int composante = random.nextInt(3);
            int index = position * 3 + composante;
            
            if (!posOccupees[index]) {
                positions[posCount][0] = x;
                positions[posCount][1] = y;
                positions[posCount][2] = composante;
                posOccupees[index] = true;
                posCount++;
            }
        }
        
        // Cacher les données dans l'image
        for (int i = 0; i < totalBits; i++) {
            int x = positions[i][0];
            int y = positions[i][1];
            int composante = positions[i][2];
            
            // Obtenir la couleur RGB du pixel
            int pixel = image.getRGB(x, y);
            
            int rouge = (pixel >> 16) & 0xff;
            int vert = (pixel >> 8) & 0xff;
            int bleu = pixel & 0xff;
            int alpha = (pixel >> 24) & 0xff;
            
            // Remplacer le LSB de la composante sélectionnée
            char bit = donneesCompletes.charAt(i);
            int valeurBit = (bit == '1') ? 1 : 0;
            
            switch (composante) {
                case 0: // Rouge
                    rouge = (rouge & 0xFE) | valeurBit;
                    break;
                case 1: // Vert
                    vert = (vert & 0xFE) | valeurBit;
                    break;
                case 2: // Bleu
                    bleu = (bleu & 0xFE) | valeurBit;
                    break;
            }
            
            // Reconstituer le pixel
            int nouveauPixel = (alpha << 24) | (rouge << 16) | (vert << 8) | bleu;
            image.setRGB(x, y, nouveauPixel);
        }
        
        // Enregistrer l'image modifiée
        ImageIO.write(image, "png", new File(cheminSortie));
        
        System.out.println("Message caché avec succès (méthode aléatoire). Bits utilisés: " + totalBits + 
            " sur " + (largeur * hauteur * 3) + " disponibles.");
    }
} 