//-----------------------------------------------------------------------------
//
// Purpose: Single-threaded Huffman Encoder 
//
//-----------------------------------------------------------------------------

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.HashMap;
import java.io.File;

public class HuffmanSingleThreaded 
{
	public static void main( String args[] ) throws Exception
	{	
		long startTime = System.nanoTime();	// Time tree building
	
		Map<String, Integer> dict = new HashMap<>();
		String currentLine = new String();
		BufferedReader br = new BufferedReader( new FileReader( "const.txt" ) );	
		while ( ( currentLine = br.readLine() ) != null )
		{
			// Delimiter
			String words[] = currentLine.split( " " );
			
			for ( int i = 0; i < words.length; i++ )
			{
				if ( !words[i].isEmpty() )
				{
					// Add or increment table values
					dict.merge( words[i], 1, Integer::sum );
				}
			}
		}
		br.close();
		
		HuffmanTree tree = new HuffmanTree();
		tree.CreateTree( dict );
		tree.BuildCodeTable();
		
		long endTime = System.nanoTime();		
		System.out.println( "Time to build tree: " + ( ( double ) endTime - ( double ) startTime ) / 1000000000 + "sec" );
		startTime = System.nanoTime(); // Time encoding
		
		// Begin encoding
		br = new BufferedReader( new FileReader( "const.txt" ) );
		FileOutputStream out = new FileOutputStream( "const_encoded.txt" );
		
		// Store in bytes
		byte b = 0x00;
		int count = 0;
		
		while ( ( currentLine = br.readLine() ) != null )
		{
			// Delimiter
			String words[] = currentLine.split( " " );
			
			for ( int i = 0; i < words.length; i++ )
			{			
				String code = tree.getCode( words[i] );
				if ( code != null )
				{
					for ( int j = 0; j < code.length(); j++ )
					{
						// Check if we've filled a byte
						if ( count > 7 )
						{
							// Write the byte
							out.write( new byte[]{b} );
							b = 0x00;
							count = 0;
						}
						
						// Set bit
						b |= ( code.charAt( j ) == '0' ) ? 0 << j : 1 << j;
						count++;
					}
				}
			}
		}
		br.close();
		out.close();
		
		endTime = System.nanoTime();
		System.out.println( "Time to encode: " + ( ( double ) endTime - ( double ) startTime ) / 1000000000 + "sec" );
	
		// Get compression percent
		File f = new File( "const.txt" );
		long fileSize = f.length();
		f = new File( "const_encoded.txt" );
		
		// Print compression
		System.out.println( "Encoded file is " + ( 100 - ( ( 100 / ( double ) fileSize ) * ( double ) f.length() ) ) + " More compressed" );
		
		System.exit( 0 );
	}
}

class HuffmanTree
{
	private Map<String, String> huffmanCodes; // Words map to codes
	private HuffmanNode root; // root node
	
	public HuffmanTree()
	{
		huffmanCodes = new HashMap<>();
	}
	
	// Get codes
	public String getCode( String word )
	{
		return huffmanCodes.get( word );
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
			
			if ( !current.getWord().isEmpty() )
			{
				huffmanCodes.put( current.getWord(), code );
			}
			
			inOrder( current.getRight(), code + "1" );		
		}
	}
	
	// Convert hashmap to tree structure
	public void CreateTree( Map<String, Integer> dictionary )
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
	private String 		word;
	private int 		count;
	private HuffmanNode left;
	private HuffmanNode right;
	
	// Merge constructor
	public HuffmanNode( HuffmanNode left, HuffmanNode right, int count )
	{
		this.word = "";
		this.count = count;
		this.left = left;
		this.right = right;
	}
	
	// Dictionary constructor
	public HuffmanNode( String word, int count )
	{
		this.word = word;
		this.count = count;
		this.left = null;
		this.right = null;
	}
	
	public void			setWord( String word ) 			{ this.word = word; }
	public void			setCount( int count ) 			{ this.count = count; }
	public void			setLeft( HuffmanNode left ) 	{ this.left = left; }
	public void			setRight( HuffmanNode right ) 	{ this.right = right; }

	public String 		getWord() 	{ return word; }
	public int	 	   	getCount() 	{ return count; }
	public HuffmanNode 	getLeft() 	{ return left; }
	public HuffmanNode 	getRight() 	{ return right; }
}
