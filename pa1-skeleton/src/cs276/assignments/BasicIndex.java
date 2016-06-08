package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class BasicIndex implements BaseIndex {

    private static final int INT_BYTES = Integer.SIZE / Byte.SIZE;

    @Override
    public PostingList readPosting(FileChannel fc) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(INT_BYTES * 2);
        if (fc.read(buf) == -1) return null;

        buf.flip();
        int termId = buf.getInt();
        int docFreq = buf.getInt();

        List<Integer> postings = new ArrayList<Integer>(docFreq);
        buf = ByteBuffer.allocate(INT_BYTES * docFreq);
        fc.read(buf);

        buf.flip();
        for (int i = 0; i < docFreq; i++) {
            postings.add(buf.getInt());
        }

        PostingList posting = new PostingList(termId, postings);
        return posting;
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(INT_BYTES * (p.getList().size() + 2));
        buf.putInt(p.getTermId());
        buf.putInt(p.getList().size());
        for (int docId : p.getList()) {
            buf.putInt(docId);
        }
        buf.flip();
        fc.write(buf);
    }
}
