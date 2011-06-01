package com.ryanm.minedroid;

import com.ryanm.droid.rugl.geom.ColouredShape;
import com.ryanm.droid.rugl.geom.Shape;
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
import com.ryanm.minedroid.BlockFactory.Block;

/**
 * @author ryanm
 */
public class ItemFactory
{
	private static Texture itemTexture;

	/**
	 * For rendering items in the inventory and hand
	 */
	public static State itemState = GLUtil.typicalState.with( MinFilter.NEAREST,
			MagFilter.NEAREST );

	/**
	 * Loads the item texture
	 */
	public static void loadTexture()
	{
		ResourceLoader.loadNow( new BitmapLoader( R.drawable.items ){
			@Override
			public void complete()
			{
				itemTexture = TextureFactory.buildTexture( resource, true, false );

				if( itemTexture != null )
					itemState = itemTexture.applyTo( itemState );
			}
		} );
	}

	/**
	 * Stuff that can appear in your inventory
	 * 
	 * @author ryanm
	 */
	public enum Item
	{
		/***/
		DiamondPick( 3, 6 ) {
			@Override
			public boolean isAppropriateTool( final Block b )
			{
				return Block.Cobble == b || Block.Stone == b;
			}
		},
		/***/
		DiamondShovel( 3, 5 ) {
			@Override
			public boolean isAppropriateTool( final Block b )
			{
				return Block.Dirt == b || Block.Grass == b || Block.Sand == b
						|| Block.Gravel == b;
			}
		},
		/***/
		DiamondSword( 3, 4 ),
		/***/
		DiamondAxe( 3, 7 ) {
			@Override
			public boolean isAppropriateTool( final Block b )
			{
				return Block.Wood == b || Block.Log == b;
			}
		},
		/***/
		Grass( Block.Grass ),
		/***/
		Cobble( Block.Cobble ),
		/***/
		Dirt( Block.Dirt ),
		/***/
		Wood( Block.Wood ),
		/***/
		Log( Block.Log ),
		/***/
		Glass( Block.Glass );

		/**
		 * 1-unit high, origin-centered, shape with the appropriate texture
		 */
		public final TexturedShape itemShape;

		/**
		 * The block type represented by this item
		 */
		public final Block block;

		private Item( final int s, final int t )
		{
			final float[] texCoords =
					ShapeUtil
							.vertFlipQuadTexCoords( ShapeUtil.getQuadTexCoords( 1 ) );
			for( int i = 0; i < texCoords.length; i += 2 )
			{
				texCoords[ i ] += s;
				texCoords[ i + 1 ] += t;

				texCoords[ i ] /= 16;
				texCoords[ i + 1 ] /= 16;
			}

			final Shape shape = ShapeUtil.filledQuad( -0.5f, -0.5f, 0.5f, 0.5f, 0 );
			final ColouredShape cs =
					new ColouredShape( shape, Colour.white, itemState );
			itemShape = new TexturedShape( cs, texCoords, itemTexture );
			block = null;
		}

		private Item( final Block block )
		{
			itemShape = block.blockItemShape;
			this.block = block;
		}

		/**
		 * @param b
		 * @return <code>true</code> if this tool is appropriate to break the
		 *         given block
		 */
		@SuppressWarnings( "static-method" )
		public boolean isAppropriateTool( final Block b )
		{
			return false;
		}
	}
}
