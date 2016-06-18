package edu.stanford.cs276;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class CandidateGenerator implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int editDistanceLimit = Config.cgEditDistanceLimit;
    private static LanguageModel lm_;
    private static CandidateGenerator cg_;

    // Don't use the constructor since this is a Singleton instance
    private CandidateGenerator() {
    }

    public static CandidateGenerator get() throws Exception {
        if (cg_ == null) {
            cg_ = new CandidateGenerator();
            lm_ = LanguageModel.load();
        }
        return cg_;
    }

    public static final Character[] alphabet = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', ',' };

    // Generate all candidates for the target query
    public Set<String> getCandidates(String query) throws Exception {
        Set<String> candidates = getCandidates(query, editDistanceLimit);
        if (lm_.noOfInvalidTerms(query) == 0) candidates.add(query);
        return candidates;
    }

    private Set<String> getCandidates(String query, int editDistanceLimit) {
        Set<String> candidates = new HashSet<String>();
        Set<String> possibleCandidates = new HashSet<String>();
        StringBuilder candidateSb = new StringBuilder(query);
        for (int i = 0; i <= query.length(); i++) {
            char origC = i < query.length() ? query.charAt(i) : ' ';
            for (char newC : alphabet) {
                String insert = null, delete = null, replace = null, transpose = null;
                insert = candidateSb.insert(i, newC).toString();
                candidateSb.deleteCharAt(i);
                if (i < query.length()) {
                    delete = candidateSb.deleteCharAt(i).toString();
                    replace = candidateSb.insert(i, newC).toString();
                    if (i > 0) {
                        candidateSb.setCharAt(i, candidateSb.charAt(i - 1));
                        candidateSb.setCharAt(i - 1, origC);
                        transpose = candidateSb.toString();
                        candidateSb.setCharAt(i - 1, candidateSb.charAt(i));
                        candidateSb.setCharAt(i, origC);
                    }
                }
                for (String candidate : new String[] { insert, delete, replace, transpose }) {
                    if (candidate == null) continue;
                    possibleCandidates.add(candidate);
                }
            }
        }

        for (String possibleCandidate : possibleCandidates) {
            int noOfInvalidTerm = lm_.noOfInvalidTerms(possibleCandidate);
            if (noOfInvalidTerm == 0) candidates.add(possibleCandidate);
            if (noOfInvalidTerm <= editDistanceLimit - 1 && editDistanceLimit > 1) {
                candidates.addAll(getCandidates(possibleCandidate, editDistanceLimit - 1));
            }
        }
        return candidates;
    }

    public static void main(String[] args) throws Exception {
        CandidateGenerator cg = CandidateGenerator.get();
        long startTime = System.nanoTime();
        System.out.println(cg.getCandidates("i wat apple").size());
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }

}
