package cs276.assignments;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PostingList {

    private int termId;
    /* A list of docIDs (i.e. postings) */
    private List<Integer> postings;

    public PostingList(int termId, List<Integer> list) {
        this.termId = termId;
        this.postings = list;
    }

    public PostingList(int termId) {
        this.termId = termId;
        this.postings = new ArrayList<Integer>();
    }

    public int getTermId() {
        return this.termId;
    }

    public List<Integer> getList() {
        return this.postings;
    }

    private static <X> X popNextOrNull(Iterator<X> p) {
        return p.hasNext() ? p.next() : null;
    }

    public static PostingList merge(PostingList p1, PostingList p2) {
        //Check that both lists aren't empty
        if (p1.getList().isEmpty()) return p2;
        if (p2.getList().isEmpty()) return p1;
        //Build new posting list
        PostingList p = new PostingList(p1.getTermId());
        Iterator<Integer> iter1 = p1.getList().iterator();
        Iterator<Integer> iter2 = p2.getList().iterator();
        Integer docId1 = popNextOrNull(iter1);
        Integer docId2 = popNextOrNull(iter2);
        while (docId1 != null && docId2 != null) {
            if (docId1.equals(docId2)) {
                p.getList().add(docId1);
                docId1 = popNextOrNull(iter1);
                docId2 = popNextOrNull(iter2);
            } else if (docId1 < docId2) {
                p.getList().add(docId1);
                docId1 = popNextOrNull(iter1);
            } else {
                p.getList().add(docId2);
                docId2 = popNextOrNull(iter2);
            }
        }
        while (docId1 != null) {
            p.getList().add(docId1);
            docId1 = popNextOrNull(iter1);
        }
        while (docId2 != null) {
            p.getList().add(docId2);
            docId2 = popNextOrNull(iter2);
        }
        return p;
    }

    public static List<Integer> intersect(List<Integer> list, PostingList p) {
        List<Integer> res = new ArrayList<Integer>();
        if (p.getList().isEmpty()) return res;
        Iterator<Integer> iter1 = list.iterator();
        Iterator<Integer> iter2 = p.getList().iterator();
        Integer docId1 = popNextOrNull(iter1);
        Integer docId2 = popNextOrNull(iter2);
        while (docId1 != null && docId2 != null) {
            if (docId1 == docId2) {
                res.add(docId1);
                docId1 = popNextOrNull(iter1);
                docId2 = popNextOrNull(iter2);
            } else if (docId1 < docId2) {
                docId1 = popNextOrNull(iter1);
            } else {
                docId2 = popNextOrNull(iter2);
            }
        }
        return res;
    }
}
