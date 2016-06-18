package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.stanford.cs276.util.Dictionary;

/**
 * LanguageModel class constructs a language model from the training corpus.
 * This model will be used to score generated query candidates.
 * 
 * This class uses the Singleton design pattern
 * (https://en.wikipedia.org/wiki/Singleton_pattern).
 */
public class LanguageModel implements Serializable {

    private static final long serialVersionUID = 1L;
    private static LanguageModel lm_;

    private static double lambda = 0.0;
    private Dictionary unigram = new Dictionary();
    private Dictionary bigram = new Dictionary();

    /*
     * Feel free to add more members here (e.g., a data structure that stores
     * bigrams)
     */

    /**
     * Constructor Do not call constructor directly from outside this class
     * since this is a Singleton class
     */
    private LanguageModel(String corpusFilePath) throws Exception {
        constructDictionaries(corpusFilePath);
    }

    private String getBigramString(String term1, String term2) {
        return term1 + " " + term2;
    }

    /**
     * This method computes language model parameters (i.e. counts of unigrams,
     * bigrams etc)
     */
    public void constructDictionaries(String corpusFilePath) throws Exception {

        System.out.println("Constructing dictionaries...");
        File dir = new File(corpusFilePath);
        for (File file : dir.listFiles()) {
            if (".".equals(file.getName()) || "..".equals(file.getName()) || file.isHidden()) {
                continue; // Ignore the self and parent aliases.
            }
            System.out.printf("Reading data file %s ...\n", file.getName());
            BufferedReader input = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = input.readLine()) != null) {
                /*
                 * Remember: each line is a document (refer to PA2 handout)
                 * TODO: Your code here
                 */
                String[] tokens = line.split("\\s+");
                for (int i = 0; i < tokens.length - 1; i++) {
                    unigram.add(tokens[i]);
                    bigram.add(getBigramString(tokens[i], tokens[i + 1]));
                }
                unigram.add(tokens[tokens.length - 1]);
            }
            input.close();
        }
        System.out.println("Done.");
    }

    public static void setSmoothingLambda(double lambda) {
        LanguageModel.lambda = lambda;
    }

    public double getUnigramProb(String term) {
        return (double) unigram.count(term) / unigram.termCount();
    }

    public double getBigramProb(String term1, String term2) {
        return lambda * getUnigramProb(term1) + (1 - lambda) * bigram.count(getBigramString(term1, term2)) / unigram.count(term1);
    }

    /**
     * Loads the language model object (and all associated data) from disk
     */
    public static LanguageModel load() throws Exception {
        try {
            if (lm_ == null) {
                FileInputStream fiA = new FileInputStream(Config.languageModelFile);
                ObjectInputStream oisA = new ObjectInputStream(fiA);
                lm_ = (LanguageModel) oisA.readObject();
            }
        } catch (Exception e) {
            throw new Exception("Unable to load language model.  You may have not run build corrector");
        }
        return lm_;
    }

    /**
     * Saves the object (and all associated data) to disk
     */
    public void save() throws Exception {
        FileOutputStream saveFile = new FileOutputStream(Config.languageModelFile);
        ObjectOutputStream save = new ObjectOutputStream(saveFile);
        save.writeObject(this);
        save.close();
    }

    /**
     * Creates a new LanguageModel object from a corpus. This method should be
     * used to create a new object rather than calling the constructor directly
     * from outside this class
     */
    public static LanguageModel create(String corpusFilePath) throws Exception {
        if (lm_ == null) {
            lm_ = new LanguageModel(corpusFilePath);
        }
        return lm_;
    }

    //    public static void main(String[] args) throws Exception {
    //        //        LanguageModel lm = new LanguageModel("/Users/YunchenWei/Documents/Coursera/IR/pa2-data/corpus");
    //        LanguageModel.load();
    //        LanguageModel lm = create("/Users/YunchenWei/Documents/Coursera/IR/pa2-data/corpus");
    //        System.out.println(lm.getUnigramProb("test"));
    //        System.out.println(lm.getUnigramProb("i"));
    //        System.out.println(lm.getBigramProb("i", "want"));
    //    }
}
