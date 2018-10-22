//-----------------------------------------------------------------------------
//
// Purpose: Single-threaded Huffman Encoder 
//
//-----------------------------------------------------------------------------

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.BitSet;
import java.util.HashMap;
import java.io.File;

public class Huffman 
{
	public static void main( String args[] ) throws Exception
	{	
		long startTime = System.nanoTime();	// Time tree building
	
		Map<Character, Integer> dict = new HashMap<>();
		
		// Parse file
		String strFile = new String( Files.readAllBytes( Paths.get( "const.txt" ) ) );
		
		// Create threads;
		Thread t[] = new Thread[4];
		for ( int i = 0; i < 4; i++ )
		{
			t[i] = new Thread( new ParseThread( dict, strFile.substring( i * ( strFile.length() / 4 ), ( i + 1 ) * ( strFile.length() / 4 ) ) ) );
			t[i].start();
		}
		
		// Wait for threads
		for ( Thread thread : t )
		{
			thread.join();
		}
				
		HuffmanTree tree = new HuffmanTree();
		tree.CreateTree( dict );
		tree.BuildCodeTable();
		
		long endTime = System.nanoTime();		
		System.out.println( "Time to build tree: " + ( ( double ) endTime - ( double ) startTime ) / 1000000000 + "sec" );
		startTime = System.nanoTime(); // Time encoding
		
		// Begin encoding
		FileOutputStream out = new FileOutputStream( "const_encoded.txt" );
		
		// Create threads;
		FutureTask<BitSet> ft[] = new FutureTask[4];
		for ( int i = 0; i < 4; i++ )
		{
			ft[i] = new FutureTask<>( new EncodeThread( strFile.substring( i * ( strFile.length() / 4 ), ( i + 1 ) * ( strFile.length() / 4 ) ), tree ) );
			t[i] = new Thread( ft[i] );
			t[i].start();
		}
		
		// Store bits
		BitSet bits[] = new BitSet[4];
		for ( int i =0; i < 4; i++ )
		{
			bits[i] = ft[i].get();
			out.write( bits[i].toByteArray() );;
		}
		
		out.close();
		
		endTime = System.nanoTime();
		System.out.println( "Time to encode: " + ( ( double ) endTime - ( double ) startTime ) / 1000000000 + "sec" );
	
		// Get compression percent
		File f = new File( "const.txt" );
		long fileSize = f.length();
		f = new File( "const_encoded.txt" );
		
		// Print compression
		System.out.println( "Encoded file is %" + ( 100 - ( ( 100 / ( float ) fileSize ) * ( float ) f.length() ) ) + " More compressed" );
		
		tree.StoreCodeTable();
		
		System.exit( 0 );
	}
}

class HuffmanTree
{
	private Map<Character, String> huffmanCodes; // Words map to codes
	private HuffmanNode root; // root node
	
	public HuffmanTree()
	{
		huffmanCodes = new HashMap<>();
	}
	
	// Get codes
	public String getCode( Character word )
	{
		return huffmanCodes.get( word );
	}
	
	public void StoreCodeTable() throws IOException
	{
		BufferedWriter bw = new BufferedWriter( new FileWriter( "const_codetable.txt" ) );
		
		// Store code table
		huffmanCodes.forEach( ( k, v ) -> 
		{
			try 
			{
				bw.write( k );
				bw.newLine();
				bw.write( v );
				bw.newLine();
			} 
			catch ( IOException e ) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} );
		
		bw.close();
	}
	
	// Create our code table
	public void BuildCodeTable()
	{
		// Inorder traversal
		inOrder( root, "" );
	}
	
	private void inOrder( HuffmanNode current, String code )
	{
		if ( current != null )
		{
			inOrder( current.getLeft(), code + "0" );
			
			if ( current.getWord() != '\0' )
			{
				huffmanCodes.put( current.getWord(), code );
			}
			
			inOrder( current.getRight(), code + "1" );		
		}
	}
	
	// Convert hashmap to tree structure
	public void CreateTree( Map<Character, Integer> dictionary )
	{
		// Sorting 
		// Lambda expression sorts nodes in ascending order (min. to max.)
		Queue<HuffmanNode> q = new PriorityQueue<>( ( left, right ) -> left.getCount() - right.getCount() );
	
		// Add base nodes to queue
		dictionary.forEach( ( k, v ) -> 
		{
			q.add( new HuffmanNode( k, v ) );
		} );
		
		// Keep merging our nodes until only one remains
		while ( q.size() > 1 )
		{
			// Grab the next two nodes
			HuffmanNode left = q.poll(), right = q.poll();
			
			// Merge
			q.add( new HuffmanNode( left, right, left.getCount() + right.getCount() ) );
		}
		
		// Product of merging is our root
		root = q.poll();
	}
}

class HuffmanNode
{
	private Character 	word;
	private int 		count;
	private HuffmanNode left;
	private HuffmanNode right;
	
	// Merge constructor
	public HuffmanNode( HuffmanNode left, HuffmanNode right, int count )
	{
		this.word = '\0';
		this.count = count;
		this.left = left;
		this.right = right;
	}
	
	// Dictionary constructor
	public HuffmanNode( Character word, int count )
	{
		this.word = word;
		this.count = count;
		this.left = null;
		this.right = null;
	}
	
	public void			setWord( Character word ) 		{ this.word = word; }
	public void			setCount( int count ) 			{ this.count = count; }
	public void			setLeft( HuffmanNode left ) 	{ this.left = left; }
	public void			setRight( HuffmanNode right ) 	{ this.right = right; }

	public Character 	getWord() 	{ return word; }
	public int	 	   	getCount() 	{ return count; }
	public HuffmanNode 	getLeft() 	{ return left; }
	public HuffmanNode 	getRight() 	{ return right; }
}

class ParseThread implements Runnable
{
	private Map<Character, Integer> dictionary;
	private String partition;
	
	public ParseThread( Map<Character, Integer> dictionary, String partition )
	{
		this.dictionary = dictionary;
		this.partition = partition;
	}
	
	public void run()
	{
		for ( int i = 0; i < partition.length(); i++ )
		{
			synchronized ( dictionary )
			{
				// Add or increment table values
				dictionary.merge( partition.charAt( i ), 1, Integer::sum );
			}
		}
	}
}

class EncodeThread implements Callable<BitSet>
{
	private String partition;
	private HuffmanTree tree;
	
	public EncodeThread( String partition, HuffmanTree tree )
	{
		this.partition = partition;
		this.tree = tree;
	}
	
	public BitSet call()
	{
		// Store in bytes
		BitSet b = new BitSet();
		int bitCount = 0;
		
		for ( int i = 0; i < partition.length(); i++ )
		{	
			String code = tree.getCode( partition.charAt( i ) );
			if ( code != null )
			{
				for ( int j = 0; j < code.length(); j++ )
				{					
					// Set bit
					b.set( bitCount, code.charAt( j ) == '1'  ? true : false );
					bitCount++;
				}
			}
		}
		
		return b;
	}
}