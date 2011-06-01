package com.ryanm.minedroid;

import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.Shape;
import com.ryanm.droid.rugl.geom.ShapeBuilder;
import com.ryanm.droid.rugl.geom.ShapeUtil;
import com.ryanm.droid.rugl.geom.TexturedShape;
import com.ryanm.droid.rugl.gl.GLUtil;
import com.ryanm.droid.rugl.gl.State;
import com.ryanm.droid.rugl.gl.enums.MagFilter;
import com.ryanm.droid.rugl.gl.enums.MinFilter;
import com.ryanm.droid.rugl.res.BitmapLoader;
import com.ryanm.droid.rugl.res.ResourceLoader;
import com.ryanm.droid.rugl.texture.Texture;
import com.ryanm.droid.rugl.texture.TextureFactory;
import com.ryanm.droid.rugl.util.Colour;
import com.ryanm.droid.rugl.util.Trig;

/**
 * Defines block data, can add face geometry to {@link ShapeBuilder}s. Thanks go
 * to Richard Invent for the missing block definitions.
 * 
 * @author ryanm
 */
public class BlockFactory
{
	private static final float[] nbl = new float[] { 0, 0, 0 };

	private static final float[] ntl = new float[] { 0, 1, 0 };

	private static final float[] nbr = new float[] { 1, 0, 0 };

	private static final float[] ntr = new float[] { 1, 1, 0 };

	private static final float[] fbl = new float[] { 0, 0, 1 };

	private static final float[] ftl = new float[] { 0, 1, 1 };

	private static final float[] fbr = new float[] { 1, 0, 1 };

	private static final float[] ftr = new float[] { 1, 1, 1 };

	private static final float sxtn = 1.0f / 16;

	private static final ColouredShape itemShape;
	static
	{
		float[] hexVerts = new float[2 * 7];
		hexVerts[ 0 ] = 0;
		hexVerts[ 1 ] = 0;

		float[] angles = new float[] { 30, 90, 150, 210, 270, 330 };
		for( int i = 0; i < angles.length; i++ )
		{
			hexVerts[ 2 * ( i + 1 ) ] =
					0.5f * Trig.cos( Trig.toRadians( angles[ i ] ) );
			hexVerts[ 2 * ( i + 1 ) + 1 ] =
					0.5f * Trig.sin( Trig.toRadians( angles[ i ] ) );
		}

		float[] itemCoords = new float[36];
		int i = 0;
		// top
		addVert( itemCoords, i++, hexVerts, 0 );
		addVert( itemCoords, i++, hexVerts, 3 );
		addVert( itemCoords, i++, hexVerts, 1 );
		addVert( itemCoords, i++, hexVerts, 2 );

		// left
		addVert( itemCoords, i++, hexVerts, 4 );
		addVert( itemCoords, i++, hexVerts, 3 );
		addVert( itemCoords, i++, hexVerts, 5 );
		addVert( itemCoords, i++, hexVerts, 0 );

		// right
		addVert( itemCoords, i++, hexVerts, 5 );
		addVert( itemCoords, i++, hexVerts, 0 );
		addVert( itemCoords, i++, hexVerts, 6 );
		addVert( itemCoords, i++, hexVerts, 1 );

		short[] tris = ShapeUtil.makeQuads( 12, 0, null, 0 );

		int[] colours = new int[12];
		int top = Colour.packFloat( 1, 1, 1, 1 );
		int left = Colour.packFloat( 0.75f, 0.75f, 0.75f, 1 );
		int right = Colour.packFloat( 0.5f, 0.5f, 0.5f, 1 );
		for( i = 0; i < 4; i++ )
		{
			colours[ i ] = top;
			colours[ i + 4 ] = left;
			colours[ i + 8 ] = right;
		}

		itemShape =
				new ColouredShape( new Shape( itemCoords, tris ), colours, null );
	}

	private static void addVert( float[] itemVerts, int index,
			float[] hexCoords, int vertIndex )
	{
		itemVerts[ 3 * index ] = hexCoords[ 2 * vertIndex ];
		itemVerts[ 3 * index + 1 ] = hexCoords[ 2 * vertIndex + 1 ];
		itemVerts[ 3 * index + 2 ] = 0;
	}

	/**
	 * The terrain.png texture
	 */
	public static Texture texture;

	/**
	 * Rendering state for blocks. Texture filtering is for wimps
	 */
	public static State state = GLUtil.typicalState.with( MinFilter.NEAREST,
			MagFilter.NEAREST );// .with( new Fog( FogMode.LINEAR, 1,
										// 60f, 80f, Colour.white ) );

	/**
	 * Synchronously loads the terrain texture
	 */
	public static void loadTexture()
	{
		ResourceLoader.loadNow( new BitmapLoader( R.drawable.terrain ){
			@Override
			public void complete()
			{
				Texture terrain =
						TextureFactory.buildTexture( resource, true, false );
				BlockFactory.texture = terrain;

				if( texture != null )
				{
					state = texture.applyTo( state );
					for( Block b : Block.values() )
						b.blockItemShape.state = state;
				}

				resource.bitmap.recycle();
			}
		} );
	}

	/**
	 * A map of block id values to {@link Block}s
	 */
	private static final Block[] blocks;
	static
	{
		int maxID = 0;

		for( Block b : Block.values() )
			maxID = Math.max( maxID, b.id );

		blocks = new Block[maxID + 1];

		for( Block b : Block.values() )
			blocks[ b.id ] = b;
	}

	/**
	 * @param id
	 * @return The so-typed block
	 */
	public static Block getBlock( byte id )
	{
		if( id >= blocks.length )
			return null;

		return blocks[ id ];
	}

	/**
	 * @param id
	 * @return <code>true</code> if the block is opaque, <code>false</code> if
	 *         transparent
	 */
	public static boolean opaque( byte id )
	{
		if( id >= blocks.length || blocks[ id ] == null )
			return false;

		return blocks[ id ].opaque;
	}

	/**
	 * Holds vertex positions for each face of a unit cube
	 * 
	 * @author ryanm
	 */
	public static enum Face
	{
		/**
		 * -ve x direction
		 */
		North( fbl, ftl, nbl, ntl ),
		/**
		 * +ve x direction
		 */
		South( nbr, ntr, fbr, ftr ),
		/**
		 * -ve z direction
		 */
		East( nbl, ntl, nbr, ntr ),
		/**
		 * +ve z direction
		 */
		West( fbr, ftr, fbl, ftl ),
		/**
		 * +ve y direction
		 */
		Top( ntl, ftl, ntr, ftr ),
		/**
		 * -ve y direction
		 */
		Bottom( nbl, fbl, nbr, fbr );

		private final float[] verts = new float[12];

		private Face( float[]... verts )
		{
			for( int i = 0; i < verts.length; i++ )
				System.arraycopy( verts[ i ], 0, this.verts, i * 3, 3 );
		}
	}

	/**
	 * Block types
	 * 
	 * @author ryanm
	 */
	public static enum Block
	{
		/***/
		Stone( ( byte ) 1, true, 1, 0 ),
		/***/
		Grass( ( byte ) 2, true, 3, 0, 0, 0, 2, 0 ),
		/***/
		Dirt( ( byte ) 3, true, 2, 0 ),
		/***/
		Cobble( ( byte ) 4, true, 0, 1 ),
		/***/
		Wood( ( byte ) 5, true, 4, 0 ),
		/***/
		Bedrock( ( byte ) 7, true, 1, 1 ),
		/***/
		Water( ( byte ) 8, false, 15, 12 ),
		/***/
		StillWater( ( byte ) 9, false, 15, 12 ),
		/***/
		Lava( ( byte ) 10, true, 15, 15 ),
		/***/
		StillLava( ( byte ) 11, true, 15, 15 ),
		/***/
		Sand( ( byte ) 12, true, 2, 1 ),
		/***/
		Gravel( ( byte ) 13, true, 3, 1 ),
		/***/
		GoldOre( ( byte ) 14, true, 0, 2 ),
		/***/
		IronOre( ( byte ) 15, true, 1, 2 ),
		/***/
		CoalOre( ( byte ) 16, true, 2, 2 ),
		/***/
		Log( ( byte ) 17, true, 4, 1, 5, 1 ),
		/***/
		Leaves( ( byte ) 18, false, 4, 3 ),
		/***/
		Sponge( ( byte ) 19, true, 0, 3 ),
		/***/
		Glass( ( byte ) 20, false, 1, 3 ),
		/***/
		LapisOre( ( byte ) 21, true, 0, 10 ),
		/***/
		Lapis( ( byte ) 22, true, 0, 9 ),
		/***/
		Dispenser( ( byte ) 23, true, 13, 2, 13, 2, 13, 2, 14, 2, 1, 0, 1, 0 ),
		/***/
		SandStone( ( byte ) 24, true, 0, 12, 0, 11, 0, 13 ),
		/***/
		NoteBlock( ( byte ) 25, true, 10, 4 ),
		/***/
		Wool( ( byte ) 35, true, 0, 4 ),
		/***/
		Gold( ( byte ) 41, true, 7, 2 ),
		/***/
		Iron( ( byte ) 42, true, 6, 2 ),
		/***/
		DoubleSlab( ( byte ) 43, true, 5, 0, 6, 0 ),
		/** These things are a complete ball-ache */
		Slab( ( byte ) 44, false, 5, 0, 6, 0 ),
		/***/
		Brick( ( byte ) 45, true, 7, 0 ),
		/***/
		TNT( ( byte ) 46, true, 8, 0, 9, 0, 10, 0 ),
		/***/
		Bookshelf( ( byte ) 47, true, 3, 2 ),
		/***/
		MossyCobble( ( byte ) 48, true, 4, 2 ),
		/***/
		Obsidian( ( byte ) 49, true, 5, 2 ),
		/***/
		Chest( ( byte ) 54, true, 10, 1, 10, 1, 10, 1, 11, 1, 9, 1, 9, 1 ),
		/***/
		DiamondOre( ( byte ) 56, true, 2, 3 ),
		/***/
		Diamond( ( byte ) 57, true, 8, 1 ),
		/***/
		WorkBench( ( byte ) 58, true, 11, 3, 12, 3, 11, 3, 12, 3, 11, 2, 11, 2 ),
		/***/
		TilledEarth( ( byte ) 60, true, 2, 0, 7, 5, 2, 0 ),
		/***/
		Oven( ( byte ) 61, true, 13, 2, 13, 2, 13, 2, 12, 2, 1, 0, 1, 0 ),
		/***/
		RedstoneOre( ( byte ) 73, true, 3, 3 ),
		/***/
		SnowyGrass( ( byte ) 78, true, 4, 4, 2, 4, 2, 0 ),
		/***/
		Ice( ( byte ) 79, false, 1, 0 ),
		/***/
		Snow( ( byte ) 80, true, 1, 0 ),
		/***/
		Cactus( ( byte ) 81, false, 6, 4, 5, 4, 7, 4 ),
		/***/
		Clay( ( byte ) 82, true, 8, 4 ),
		/***/
		Jukebox( ( byte ) 84, true, 10, 4, 11, 4, 10, 4 ),
		/***/
		Pumpkin( ( byte ) 86, true, 6, 7, 6, 7, 6, 7, 7, 7, 6, 6, 6, 6 ),
		/***/
		Netherrack( ( byte ) 87, true, 7, 6 ),
		/***/
		SoulSand( ( byte ) 88, true, 8, 6 ),
		/***/
		GlowStone( ( byte ) 89, true, 9, 6 );

		/***/
		public final byte id;

		/***/
		public final boolean opaque;

		/**
		 * Sides then top, then bottom
		 */
		public final int[] texCoords;

		/**
		 * Shape with which to draw this block in the gui. It's 1 unit high and
		 * centered on the origin
		 */
		public final TexturedShape blockItemShape;

		/**
		 * @param id
		 *           block type identifier
		 * @param opaque
		 *           <code>true</code> if you can't see through the block
		 * @param tc
		 *           coordinates of the face textures in terrain.png. e.g.: grass
		 *           is (0,0), stone is (1,0), mossy cobblestone is (4,2)
		 */
		private Block( byte id, boolean opaque, int... tc )
		{
			this.id = id;
			this.opaque = opaque;
			if( tc.length == 6 )
				// (ooh matron!)
				texCoords =
						new int[] { tc[ 0 ], tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 0 ],
								tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 2 ], tc[ 3 ], tc[ 4 ],
								tc[ 5 ] };
			else if( tc.length == 4 )
				// (don't fancy yours much)
				texCoords =
						new int[] { tc[ 0 ], tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 0 ],
								tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 2 ], tc[ 3 ], tc[ 2 ],
								tc[ 3 ] };
			else if( tc.length == 2 )
				texCoords =
						new int[] { tc[ 0 ], tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 0 ],
								tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 0 ], tc[ 1 ], tc[ 0 ],
								tc[ 1 ] };
			else
				// all sides distinct
				texCoords = tc;

			float[] itc = new float[3 * 4 * 2];
			faceTexCoords( Face.Top, itc, 0 );
			faceTexCoords( Face.North, itc, 8 );
			faceTexCoords( Face.West, itc, 16 );
			blockItemShape =
					new TexturedShape( BlockFactory.itemShape, itc, texture );
		}

		private void faceTexCoords( Face face, float[] tc, int index )
		{
			int txco = 2 * face.ordinal();
			float bu = sxtn * texCoords[ txco ];
			float bv = sxtn * ( texCoords[ txco + 1 ] + 1 );
			float tu = sxtn * ( texCoords[ txco ] + 1 );
			float tv = sxtn * texCoords[ txco + 1 ];

			tc[ index++ ] = bu;
			tc[ index++ ] = bv;
			tc[ index++ ] = bu;
			tc[ index++ ] = tv;
			tc[ index++ ] = tu;
			tc[ index++ ] = bv;
			tc[ index++ ] = tu;
			tc[ index++ ] = tv;
		}

		/**
		 * Adds a face to the {@link ShapeBuilder}
		 * 
		 * @param f
		 *           which side
		 * @param bx
		 *           block coordinate
		 * @param by
		 *           block coordinate
		 * @param bz
		 *           block coordinate
		 * @param colour
		 *           Vertex colour
		 * @param sb
		 */
		public void face( Face f, float bx, float by, float bz, int colour,
				ShapeBuilder sb )
		{
			sb.ensureCapacity( 4, 2 );

			// add vertices
			System.arraycopy( f.verts, 0, sb.vertices, sb.vertexOffset,
					f.verts.length );

			for( int i = 0; i < 4; i++ )
			{
				// translation
				sb.vertices[ sb.vertexOffset++ ] += bx;
				sb.vertices[ sb.vertexOffset++ ] += by;
				sb.vertices[ sb.vertexOffset++ ] += bz;

				// colour
				sb.colours[ sb.colourOffset++ ] = colour;
			}

			// texcoords
			int txco = 2 * f.ordinal();
			float bu = sxtn * texCoords[ txco ];
			float bv = sxtn * ( texCoords[ txco + 1 ] + 1 );
			float tu = sxtn * ( texCoords[ txco ] + 1 );
			float tv = sxtn * texCoords[ txco + 1 ];

			if( id == 44 && f != Face.Bottom )
				if( f == Face.Top )
				{ // shift all down
					sb.vertices[ sb.vertexOffset - 2 ] -= 0.5f;
					sb.vertices[ sb.vertexOffset - 5 ] -= 0.5f;
					sb.vertices[ sb.vertexOffset - 8 ] -= 0.5f;
					sb.vertices[ sb.vertexOffset - 11 ] -= 0.5f;
				}
				else
				{ // shift top down
					sb.vertices[ sb.vertexOffset - 2 ] -= 0.5f;
					sb.vertices[ sb.vertexOffset - 8 ] -= 0.5f;

					// half texcoords
					bv *= 0.5f;
					tv *= 0.5f;
				}

			sb.texCoords[ sb.texCoordOffset++ ] = bu;
			sb.texCoords[ sb.texCoordOffset++ ] = bv;
			sb.texCoords[ sb.texCoordOffset++ ] = bu;
			sb.texCoords[ sb.texCoordOffset++ ] = tv;
			sb.texCoords[ sb.texCoordOffset++ ] = tu;
			sb.texCoords[ sb.texCoordOffset++ ] = bv;
			sb.texCoords[ sb.texCoordOffset++ ] = tu;
			sb.texCoords[ sb.texCoordOffset++ ] = tv;

			sb.relTriangle( 0, 2, 1 );
			sb.relTriangle( 2, 3, 1 );

			sb.vertexCount += 4;
		}
	}
}
