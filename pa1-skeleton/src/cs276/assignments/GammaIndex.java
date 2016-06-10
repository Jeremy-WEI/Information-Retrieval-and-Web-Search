package cs276.assignments;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class GammaIndex implements BaseIndex {

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

    public static int unaryEncodeInteger(int number, BitSet outputUnaryCode, int startIndex) {
        int nextIndex = startIndex + number;
        outputUnaryCode.set(startIndex, nextIndex);
        return nextIndex + 1;
    }

    public static void unaryDecodeInteger(BitSet inputUnaryCode, int startIndex, int[] numberEndIndex) {
        int nextIndex = startIndex;
        int number = 0;
        while (inputUnaryCode.get(nextIndex++)) {
            number++;
        }
        numberEndIndex[0] = number;
        numberEndIndex[1] = nextIndex;
    }

    public static int gammaEncodeInteger(int number, BitSet outputGammaCode, int startIndex) {
        int nextIndex = startIndex;
        if (number == 0) return startIndex;
        int bitCount = (int) (Math.log(number) / Math.log(2));
        nextIndex = unaryEncodeInteger(bitCount, outputGammaCode, nextIndex);
        while (bitCount > 0) {
            if ((number >> (bitCount-- - 1) & 1) == 1) outputGammaCode.set(nextIndex);
            nextIndex++;
        }
        return nextIndex;
    }

    public static void gammaDecodeInteger(BitSet inputGammaCode, int startIndex, int[] numberEndIndex) {
        unaryDecodeInteger(inputGammaCode, startIndex, numberEndIndex);
        int bitCount = numberEndIndex[0];
        int nextIndex = numberEndIndex[1];
        int number = 1;
        for (int i = 0; i < bitCount; i++, nextIndex++) {
            number = (number << 1) + (inputGammaCode.get(nextIndex) ? 1 : 0);
        }
        numberEndIndex[0] = number;
        numberEndIndex[1] = nextIndex;
    }

    @Override
    public PostingList readPosting(FileChannel fc) throws IOException {
        ByteBuffer metaBuf = ByteBuffer.allocate(2 * INT_BYTES);
        if (fc.read(metaBuf) == -1) return null;

        metaBuf.flip();
        int termId = metaBuf.getInt();
        int totalBits = metaBuf.getInt();

        ByteBuffer gapsBuf = ByteBuffer.allocate((totalBits + 7) / 8);
        fc.read(gapsBuf);
        gapsBuf.flip();

        BitSet bs = BitSet.valueOf(gapsBuf);

        int nextIndex = 0;
        int[] numberEndIndex = new int[2];
        List<Integer> gaps = new ArrayList<Integer>();
        while (nextIndex < totalBits) {
            gammaDecodeInteger(bs, nextIndex, numberEndIndex);
            gaps.add(numberEndIndex[0]);
            nextIndex = numberEndIndex[1];
        }

        System.out.println(gapDecode(gaps));
        return new PostingList(termId, gapDecode(gaps));
    }

    @Override
    public void writePosting(FileChannel fc, PostingList p) throws IOException {
        ByteBuffer metaBuf = ByteBuffer.allocate(2 * INT_BYTES);
        ByteBuffer gapsBuf = ByteBuffer.allocate(p.getList().size() * INT_BYTES);
        BitSet bs = new BitSet();

        List<Integer> gaps = gapEncode(p.getList());
        int nextIndex = 0;
        for (int gap : gaps) {
            nextIndex = gammaEncodeInteger(gap, bs, nextIndex);
        }

        int termId = p.getTermId();
        int totalBits = nextIndex;

        metaBuf.putInt(termId);
        metaBuf.putInt(totalBits);
        byte[] rawBytes = bs.toByteArray();
        byte[] gammaBytes = new byte[(totalBits + 7) / 8];
        for (int i = 0; i < rawBytes.length; i++) {
            gammaBytes[i] = rawBytes[i];
        }
        gapsBuf.put(gammaBytes);

        metaBuf.flip();
        gapsBuf.flip();

        fc.write(metaBuf);
        fc.write(gapsBuf);
    }
}
