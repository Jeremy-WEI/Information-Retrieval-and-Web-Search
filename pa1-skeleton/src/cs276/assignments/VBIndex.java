package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class VBIndex implements BaseIndex {

    public static List<Integer> gapEncode(List<Integer> docIds) {
        List<Integer> gaps = new ArrayList<Integer>(docIds);
        for (int i = docIds.size() - 1; i > 0; i--) {
            gaps.set(i, docIds.get(i) - docIds.get(i - 1));
        }
        return gaps;
    }

    public static List<Integer> gapDecode(List<Integer> gaps) {
        List<Integer> docIds = new ArrayList<Integer>(gaps);
        for (int i = 1; i < gaps.size(); i++) {
            docIds.set(i, gaps.get(i) + gaps.get(i - 1));
        }
        return docIds;
    }

    public static int VBEncodeInteger(int gap, byte[] outputVBCode) {
        int numBytes = 1;
        int tmp = gap;
        while (tmp > 127) {
            tmp >>>= 7;
            numBytes++;
        }
        for (int i = numBytes - 1; i >= 0; i--) {
            outputVBCode[i] = (byte) (gap & 0b01111111);
            gap >>>= 7;
        }
        outputVBCode[numBytes - 1] |= 0b10000000;
        return numBytes;
    }

    public static void VBDecodeInteger(byte[] inputVBCode, int startIndex, int[] numberEndIndex) {
        int number = 0;
        while (true) {
            number = (number << 7) + (inputVBCode[startIndex] & 0b01111111);
            if ((inputVBCode[startIndex++] & 0b10000000) != 0) break;
            if (startIndex >= inputVBCode.length) throw new IllegalArgumentException();
        }
        numberEndIndex[0] = number;
        numberEndIndex[1] = startIndex;
    }

    @Override
    public PostingList readPosting(FileChannel fc) throws IOException {
        ByteBuffer metaBuf = ByteBuffer.allocate(INT_BYTES * 2);

        if (fc.read(metaBuf) == -1) return null;
        metaBuf.flip();
        int termId = metaBuf.getInt();
        int totalBytes = metaBuf.getInt();

        ByteBuffer gapsBuf = ByteBuffer.allocate(totalBytes);
        fc.read(gapsBuf);
        gapsBuf.flip();

        List<Integer> gaps = new ArrayList<Integer>();
        int startIndex = 0;
        int[] numberEndIndex = new int[2];
        while (startIndex < totalBytes) {
            VBDecodeInteger(gapsBuf.array(), startIndex, numberEndIndex);
            gaps.add(numberEndIndex[0]);
            startIndex = numberEndIndex[1];
        }

        List<Integer> docIds = gapDecode(gaps);
        return new PostingList(termId, docIds);
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) throws IOException {
        ByteBuffer metaBuf = ByteBuffer.allocate(INT_BYTES * 2);
        ByteBuffer gapsBuf = ByteBuffer.allocate(INT_BYTES * p.getList().size());
        List<Integer> gaps = gapEncode(p.getList());

        int termId = p.getTermId();
        int totalBytes = 0;

        byte[] outputVBCode = new byte[INT_BYTES];
        for (int gap : gaps) {
            int numByte = VBEncodeInteger(gap, outputVBCode);
            totalBytes += numByte;
            gapsBuf.put(outputVBCode, 0, numByte);
        }

        metaBuf.putInt(termId);
        metaBuf.putInt(totalBytes);
        
        metaBuf.flip();
        gapsBuf.flip();

        fc.write(metaBuf);
        fc.write(gapsBuf);
    }
}
