package edu.stanford.cs276;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CandidateGenerator implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final double mu = Config.cgMu;
    private static final int editDistanceLimit = Config.cgEditDistanceLimit;

    private static LanguageModel lm_;
    private static CandidateGenerator cg_;
    private static NoisyChannelModel ncm_;

    // Don't use the constructor since this is a Singleton instance
    private CandidateGenerator() {
    }

    private static class CandidateWithProb implements Comparable<CandidateWithProb> {
        String candidate;
        double prob;

        public CandidateWithProb(String candidate) {
            this.candidate = candidate;
        }

        public CandidateWithProb(String candidate, double prob) {
            this.candidate = candidate;
            this.prob = prob;
        }

        @Override
        public int compareTo(CandidateWithProb o) {
            double delta = this.prob - o.prob;
            if (delta > 0) return 1;
            if (delta < 0) return -1;
            return 0;
        }

        @Override
        public int hashCode() {
            return candidate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CandidateWithProb)) return false;
            return candidate.equals(((CandidateWithProb) obj).candidate);
        }

    }

    public static CandidateGenerator get() throws Exception {
        if (cg_ == null) {
            cg_ = new CandidateGenerator();
            lm_ = LanguageModel.load();
            ncm_ = NoisyChannelModel.load();
            ncm_.setProbabilityType("uniform");
        }
        return cg_;
    }

    public static final Character[] alphabet = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ' ', ',' };

    // Generate all candidates for the target query
    public Set<CandidateWithProb> getCandidates(String query) {
        query = query.replaceAll("\\s+", " ").trim();
        Set<CandidateWithProb> candidates = getCandidates(new CandidateWithProb(query, 1.0), editDistanceLimit);
        if (lm_.noOfInvalidTerms(query) == 0) candidates.add(new CandidateWithProb(query, ncm_.editProbability(query, query)));
        return candidates;
    }

    private Set<CandidateWithProb> getCandidates(CandidateWithProb queryWithProb, int editDistanceLimit) {
        String query = queryWithProb.candidate;
        Set<CandidateWithProb> candidatesWithProb = new HashSet<CandidateWithProb>();
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
                    candidateSb.setCharAt(i, origC);
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
                    possibleCandidates.add(candidate.replaceAll("\\s+", " ").trim());
                }
            }
        }

        for (String possibleCandidate : possibleCandidates) {
            int noOfInvalidTerm = lm_.noOfInvalidTerms(possibleCandidate);
            CandidateWithProb candidateWithProb = new CandidateWithProb(possibleCandidate);
            if (noOfInvalidTerm > editDistanceLimit - 1 || candidatesWithProb.contains(candidateWithProb)) continue;
            candidateWithProb.prob = queryWithProb.prob * (ncm_.editProbability(possibleCandidate, query));
            if (noOfInvalidTerm == 0) candidatesWithProb.add(candidateWithProb);
            if (editDistanceLimit > 1) {
                Set<CandidateWithProb> nextCandidatesWithProb = getCandidates(candidateWithProb, editDistanceLimit - 1);
                for (CandidateWithProb nextCandidateWithProb : nextCandidatesWithProb) {
                    if (!candidatesWithProb.contains(nextCandidatesWithProb)) candidatesWithProb.add(nextCandidateWithProb);
                }
            }
        }
        return candidatesWithProb;
    }

    public String getBestCandidate(String query) {
        Set<CandidateWithProb> candidatesWithProb = getCandidates(query);
        for (CandidateWithProb candidateWithProb : candidatesWithProb) {
            candidateWithProb.prob *= lm_.getQueryProb(candidateWithProb.candidate);
        }
        CandidateWithProb bestCandidateWithProb = Collections.max(candidatesWithProb);
        return bestCandidateWithProb.candidate;
    }

    public static void main(String[] args) throws Exception {
        CandidateGenerator cg = CandidateGenerator.get();
        long startTime = System.nanoTime();
        System.out.println(cg.getBestCandidate("i wat apple"));
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;  //divide by 1000000 to get milliseconds.
        System.out.println(duration);
    }
}
