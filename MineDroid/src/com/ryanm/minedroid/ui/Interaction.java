package com.ryanm.minedroid.ui;

import android.util.FloatMath;

import com.ryanm.droid.rugl.input.Touch;
import com.ryanm.droid.rugl.input.Touch.Pointer;
import com.ryanm.droid.rugl.input.Touch.TouchListener;
import com.ryanm.droid.rugl.util.FPSCamera;
import com.ryanm.droid.rugl.util.geom.BoundingCuboid;
import com.ryanm.droid.rugl.util.geom.GridIterate;
import com.ryanm.droid.rugl.util.geom.Vector3f;
import com.ryanm.droid.rugl.util.geom.Vector3i;
import com.ryanm.droid.rugl.util.math.Range;
import com.ryanm.minedroid.BlockFactory;
import com.ryanm.minedroid.BlockFactory.Block;
import com.ryanm.minedroid.ItemFactory.Item;
import com.ryanm.minedroid.Player;
import com.ryanm.minedroid.World;
import com.ryanm.minedroid.chunk.Chunk;
import com.ryanm.minedroid.chunk.Chunklet;
import com.ryanm.preflect.annote.Summary;
import com.ryanm.preflect.annote.Variable;

/**
 * Handles block placement, tool use
 * 
 * @author ryanm
 */
@Variable( "Interaction" )
@Summary( "Options for placing/breaking blocks" )
public class Interaction implements TouchListener
{
	/***/
	@Variable( "Range" )
	@Summary( "Distance at which you can affect blocks" )
	public float range = 4;

	/***/
	@Variable( "Right Tool" )
	@Summary( "Time to break a block with the appropriate tool" )
	public float rightToolTime = 0.4f;

	/***/
	@Variable( "Wrong Tool" )
	@Summary( "Time to break a block with the wrong tool" )
	public float wrongToolTime = 1.6f;

	private final Player player;

	private final World world;

	private final FPSCamera camera;

	private final Hand hand;

	private Pointer touch = null;

	private Vector3f actionDirection = new Vector3f();

	private boolean targetValid = false;

	/**
	 * The coordinates of first solid block encountered in the actionDirection
	 */
	private Vector3i targetBlock = new Vector3i();

	/**
	 * The coordinates of the empty block we encountered just before the target
	 */
	private Vector3i placementTargetBlock = new Vector3i();

	private GridIterate gridIterate = new GridIterate();

	private BoundingCuboid blockBounds = new BoundingCuboid( 0, 0, 0, 0, 0, 0 );

	private Item sweptItem = null;

	private Block targettedBreaking;

	private Vector3i breakingLocation = new Vector3i();

	private float breakingProgress = 0;

	private boolean constantStriking = false;

	private boolean justBroken = false;

	/**
	 * Indicates if one of the touchsticks is being tap-held
	 */
	public boolean touchSticksHeld = false;

	/**
	 * @param player
	 * @param world
	 * @param camera
	 * @param hand
	 */
	public Interaction( Player player, World world, FPSCamera camera, Hand hand )
	{
		this.player = player;
		this.world = world;
		this.camera = camera;
		this.hand = hand;
	}

	/**
	 * @param delta
	 */
	public void advance( float delta )
	{
		world.setBlockPlacePreview( false, 0, 0, 0 );

		hand.stopStriking();

		if( targettedBreaking != null )
		{
			hand.repeatedStrike( true );

			float dx = player.position.x - targetBlock.x + 0.5f;
			float dy = player.position.y - targetBlock.y + 0.5f;
			float dz = player.position.z - targetBlock.z + 0.5f;
			float ds = FloatMath.sqrt( dx * dx + dy * dy + dz * dz );

			if( ds > range + 1 )
				targettedBreaking = null;
			else
			{
				Item i = activeItem();
				float time =
						i == null || !i.isAppropriateTool( targettedBreaking ) ? wrongToolTime
								: rightToolTime;
				breakingProgress += delta / time;

				if( breakingProgress > 1 )
				{ // broken!
					Chunk chunk =
							world.getChunklet( targetBlock.x, targetBlock.y,
									targetBlock.z ).parent;
					chunk.setBlockTypeForPosition( targetBlock.x, targetBlock.y,
							targetBlock.z, ( byte ) 0 );

					targettedBreaking = null;
					hand.stopStriking();
				}
			}
		}
		else if( constantStriking || touchSticksHeld )
		{
			hand.repeatedStrike( false );
			held( 400, 240, delta );
		}
		else if( touch != null && ( player.inHand != null || sweptItem != null ) )
		{
			targettedBreaking = null;
			constantStriking = false;

			if( sweptItem == null )
				hand.repeatedStrike( false );

			held( touch.x, touch.y, delta );

			world.setBlockPlacePreview( targetValid && activeItem().block != null,
					placementTargetBlock.x, placementTargetBlock.y,
					placementTargetBlock.z );
		}
	}

	private Item activeItem()
	{
		return sweptItem != null ? sweptItem : player.inHand;
	}

	/**
	 * Called when an item is dragged from the hotbar
	 * 
	 * @param item
	 * @param sweptTouch
	 */
	public void swipeFromHotBar( Item item, Touch.Pointer sweptTouch )
	{
		if( touch == null )
		{
			sweptItem = item;
			touch = sweptTouch;
		}
	}

	@Override
	public boolean pointerAdded( Pointer p )
	{
		if( touch == null )
		{
			touch = p;

			return true;
		}

		return false;
	}

	@Override
	public void pointerRemoved( Pointer p )
	{
		if( touch == p )
		{
			touch = null;

			if( !justBroken )
				action( activeItem(), p.x, p.y );

			sweptItem = null;
			justBroken = false;
		}
	}

	@Override
	public void reset()
	{
		touch = null;
		sweptItem = null;
		justBroken = false;
	}

	/**
	 * Called when the touchsticks are tapped, and when the screen is released.
	 * 
	 * @param item
	 * @param x
	 *           in pixels
	 * @param y
	 *           in pixels
	 */
	public void action( Item item, float x, float y )
	{
		if( item == null )
			return;

		Chunk chunk = updateTarget( x, y );

		hand.strike( false );

		if( chunk != null && targetValid )
			if( item.block != null )
			{ // place
				blockBounds.set( placementTargetBlock.x, placementTargetBlock.y,
						placementTargetBlock.z, placementTargetBlock.x + 1,
						placementTargetBlock.y + 1, placementTargetBlock.z + 1 );

				if( !player.playerBounds.intersects( blockBounds ) )
				{
					hand.strike( true );
					chunk.setBlockTypeForPosition( placementTargetBlock.x,
							placementTargetBlock.y, placementTargetBlock.z,
							item.block.id );
				}
			}
			else
			{ // break
				Block b =
						BlockFactory.getBlock( chunk.blockTypeForPosition(
								targetBlock.x, targetBlock.y, targetBlock.z ) );

				if( item.isAppropriateTool( b ) )
				{
					breakingProgress = 0;
					targettedBreaking = b;
					breakingLocation.set( targetBlock );
				}
			}
	}

	private void held( float x, float y, float delta )
	{
		Chunk chunk = updateTarget( x, y );

		if( chunk != null && sweptItem == null )
		{
			Block b =
					BlockFactory.getBlock( chunk.blockTypeForPosition(
							targetBlock.x, targetBlock.y, targetBlock.z ) );

			// screen-hold breaking
			if( breakingLocation.equals( targetBlock ) )
			{
				Item i = activeItem();

				boolean approp = i != null && i.isAppropriateTool( b );
				hand.repeatedStrike( approp );

				float time = approp ? rightToolTime : wrongToolTime;

				breakingProgress += delta / time;

				if( breakingProgress > 1 )
				{ // broken!
					chunk.setBlockTypeForPosition( targetBlock.x, targetBlock.y,
							targetBlock.z, ( byte ) 0 );

					targettedBreaking = null;
					hand.stopStriking();

					justBroken = true;
				}
			}
			else
			{
				breakingLocation.set( targetBlock );
				breakingProgress = 0;
			}
		}
	}

	/**
	 * @param x
	 *           in screen-space
	 * @param y
	 *           in screen-space
	 * @return The {@link Chunk} containing the targeted block
	 */
	private Chunk updateTarget( float x, float y )
	{
		x = 2 * Range.toRatio( x, 0, 800 ) - 1;
		y = 2 * Range.toRatio( y, 0, 480 ) - 1;

		// unproject
		camera.unProject( x, y, actionDirection );

		// find target block
		actionDirection.scale( range );
		gridIterate.setSeg( player.position.x, player.position.y,
				player.position.z, player.position.x + actionDirection.x,
				player.position.y + actionDirection.y, player.position.z
						+ actionDirection.z );

		targetBlock.set( gridIterate.lastGridCoords );
		placementTargetBlock.set( gridIterate.lastGridCoords );

		targetValid = false;
		Chunklet chunk = null;
		do
		{
			chunk =
					world.getChunklet( gridIterate.lastGridCoords.x,
							gridIterate.lastGridCoords.y, gridIterate.lastGridCoords.z );

			if( chunk != null )
			{
				byte bt =
						chunk.parent.blockTypeForPosition(
								gridIterate.lastGridCoords.x,
								gridIterate.lastGridCoords.y,
								gridIterate.lastGridCoords.z );

				if( bt == 0 || bt == Block.Water.id || bt == Block.StillWater.id
						|| BlockFactory.getBlock( bt ) == null )
				{
					placementTargetBlock.set( gridIterate.lastGridCoords );

					gridIterate.next();
				}
				else
				{
					// we have hit the target
					targetBlock.set( gridIterate.lastGridCoords );
					targetValid = true;
				}
			}
			else
				targetValid = false;
		}
		while( !targetValid && !gridIterate.isDone() );

		return chunk == null ? null : chunk.parent;
	}
}
