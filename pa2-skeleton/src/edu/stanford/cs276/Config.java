package edu.stanford.cs276;

public class Config {
    public static final String noisyChannelFile = "noisyChannel";
    public static final String languageModelFile = "languageModel";
    public static final String candidateGenFile = "candidateGenerator";

    public static final int cgEditDistanceLimit = 2;
    
    public static final double lmSmoothingLambda = 0.1;
    public static final double ucmErrorProb = 0.05;
    public static final double ucmCorrectProb = 0.95;
}
