import java.util.PriorityQueue;

/**
 * Spring 2020
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();

		int[] counts = readForCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);

		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root,out);

		in.reset();
		writeCompressedBits(codings,in,out);
		out.close();

	}

	/**
	 * creates an integer array freq of 257 values, reads BITS_PER_WORD
	 * chunks, and increments the frequency (value) of that chunk
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 */
	private int[] readForCounts (BitInputStream in) {
		int[] freq = new int[ALPH_SIZE + 1];
		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			freq[val] += 1;
		}
		freq[PSEUDO_EOF] = 1;
		return freq;
	}

	/**
	 * Constructs a Huffman trie from the values stored in the int[] freq
	 * by using a greedy algorithm and priority queue
	 *
	 * @param freq
	 *            integer array of incremented frequencies
	 */
	private HuffNode makeTreeFromCounts (int[] freq) {
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();

		for (int i=0; i < freq.length; i++) {
			if (freq[i] > 0) {
				pq.add(new HuffNode(i,freq[i],null,null));
			}
		}

		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(left.myValue+right.myValue, left.myWeight+right.myWeight, left, right);
			pq.add(t);
		}
		HuffNode root = pq.remove();
		return root;
	}

	/**
	 * Creates the codings for the Huffman trie to be stored in the String[] encodings
	 * Calls the method codingHelper to assign values to the indexes of encodings
	 *
	 * @param root
	 *            nodes in the Huffman trie
	 */
	private String[] makeCodingsFromTree (HuffNode root) {
		String[] encodings = new String[ALPH_SIZE+1];
		codingHelper(root,"",encodings);
		return encodings;
	}

	/**
	 * recursively assigns encodings for the values of the tree, using 0's for left and 1's for right
	 *
	 * @param root
	 *            nodes in the Huffman trie
	 * @param path
	 *            A string representing the encoding of 0's and 1's to be stored in the String array encodings
	 * @param encodings
	 * 			  The string array that stores the encodings represented by the String path
	 */
	private void codingHelper (HuffNode root, String path, String[] encodings) {
		if (root.myLeft==null && root.myRight==null) {
			encodings[root.myValue] = path;
			return;
		}
		codingHelper(root.myLeft, path + "0", encodings);
		codingHelper(root.myRight, path + "1", encodings);
	}

	/**
	 * Writes the tree by writing bits in the order of a pre-order traversal
	 *
	 * @param root
	 *            nodes in the Huffman trie
	 * @param out
	 *           Buffered bit stream writing to the output file.
	 */
	private void writeHeader (HuffNode root, BitOutputStream out) {
		if (root.myLeft!=null || root.myRight!=null) {
			out.writeBits(1,0);
			writeHeader(root.myLeft,out);
			writeHeader(root.myRight,out);
		} else {
			out.writeBits(1,1);
			out.writeBits(BITS_PER_WORD+1,root.myValue);
		}
	}

	/**
	 * Writes the compressed bits sequence following the reset of BitInputStream
	 *
	 * @param encodings
	 *            The string array that stores the encodings represented by the String path
	 * @param in
	 * 	 		 Buffered bit stream of the file to be compressed.
	 * @param out
	 *           Buffered bit stream writing to the output file.
	 */
	private void writeCompressedBits (String[] encodings, BitInputStream in, BitOutputStream out) {

		while (true) {
			int current = in.readBits(BITS_PER_WORD);
			if (current==-1) break;
			String code = encodings[current];
			if (code!=null) {
				out.writeBits(code.length(), Integer.parseInt(code, 2));
			}
		}
		String code = encodings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code,2));
	}



	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out){

//		int magic = in.readBits(BITS_PER_INT);
//		if (magic != HUFF_TREE) {
//			throw new HuffException("invalid magic number "+magic);
//		}
//		out.writeBits(BITS_PER_INT,magic);
//		while (true){
//			int val = in.readBits(BITS_PER_WORD);
//			if (val == -1) break;
//			out.writeBits(BITS_PER_WORD, val);
//		}
//		out.close();

		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("invalid magic number " + bits);
		}
		HuffNode root = readTree(in);
		HuffNode current = root;
		while (true) {
		    int bits2 = in.readBits(1);
		    if (bits2 == -1) throw new HuffException("bad input, no PSEUDO_EOF");
		    else {
		        if (bits2 == 0) current = current.myLeft;
		        else current = current.myRight;

		        if (current.myLeft==null && current.myRight==null) {
		            if (current.myValue == PSEUDO_EOF) {
		                break;
                    } else {
		                out.writeBits(8, current.myValue);
		                current = root;
                    }
                }
            }
        }
		out.close();
	}

	/**
	 * Reads the Huffman tree that is stored using a pre-order traversal
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 */
	private HuffNode readTree(BitInputStream in) {
        int bit = in.readBits(1);
        if (bit == -1) throw new HuffException("invalid magic number " + bit);
        if (bit == 0) {
            HuffNode left = readTree(in);
            HuffNode right = readTree(in);
            return new HuffNode(0,0,left,right);
        } else {
            int value = in.readBits(9);
            return new HuffNode(value,0,null,null);
        }
	}
}