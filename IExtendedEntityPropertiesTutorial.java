/**
 * Extended Entity Properties Tutorial
 */
/*
In this tutorial I will cover how to add variables to an entity by using Forge's IExtendedEntityProperties. How did I
learn all this stuff, you ask? Well, it's all pretty well documented within the Forge code itself. Hover your mouse
over most Forge methods and you'll get a great 'tooltip' pop up that explains pretty much everything about it. Or just
open up the class you're curious about. Go ahead, give it a try! If it still doesn't make sense, then read on. :)

We will add mana to all players, show how to use it, and add gold coins to all EntityLivingBase entities.

We will also use our custom data in a Gui which involves setting up a packet handler to send information to the client.

Finally, we will make it so our custom data persists even when the player dies and respawns.

If you read all the way through and follow all of the steps correctly, your custom properties will also be multi-player
compatible straight out of the box, so to speak. Tested using the Eclipse server.

Prerequisites:
1. Know how to set up and use Forge Events. See my tutorial on creating an EventHandler.
2. Willingness to read carefully.

NOTES: Updating from Forge 804 to 871
Three things you'll need to change in your GUI files:
1. I18n.func_135053_a() is now I18n.getString()
2. mc.func_110434_K() is now mc.renderEngine OR mc.getTextureManager()
3. renderEngine.func_110577_a() is now renderEngine.bindTexture()

IMPORTANT NOTE: Using IExtendedEntityProperties adds new data to entities, and the server handles initializing,
maintaining, loading and saving of all data. For multi-player compatibility outside of the Eclipse server environment,
you must require your mod to be installed server-side for the data to exist. This means your @NetworkMod line will
look like this:
*/
@NetworkMod(clientSideRequired=true, serverSideRequired=[b]true[/b], // packetHandler stuff)
/*
If you need to display any of this information, such as in a Gui, you need to send a packet to the client with the data
to display. This is the only kind of information the client needs to know.

Thanks to Pawaox for bringing this to my attention, and Seigneur_Necron for confirming the requirements.
*/
/**
 * Step 1: Create a class that implements IExtendedEntityProperties
 */
/*
Since we are first making variables specific to EntityPlayer, we will call this
class "ExtendedPlayer" so it's always obvious what kind of entity can use it. This
will be important if you add different variables to different entities.
*/
public class ExtendedPlayer implements IExtendedEntityProperties
{
	/*
	Here I create a constant EXT_PROP_NAME for this class of properties
	You need a unique name for every instance of IExtendedEntityProperties
	you make, and doing it at the top of each class as a constant makes
	it very easy to organize and avoid typos. It's easiest to keep the same
	constant name in every class, as it will be distinguished by the class
	name: ExtendedPlayer.EXT_PROP_NAME vs. ExtendedEntity.EXT_PROP_NAME
	
	Note that a single entity can have multiple extended properties, so each
	property should have a unique name. Try to come up with something more
	unique than the tutorial example.
	*/
	public final static String EXT_PROP_NAME = "ExtendedPlayer";
	
	// I always include the entity to which the properties belong for easy access
	// It's final because we won't be changing which player it is
	private final EntityPlayer player;
	
	// Declare other variables you want to add here
	
	// We're adding mana to the player, so we'll need current and max mana
	private int currentMana, maxMana;
	
	/*
	The default constructor takes no arguments, but I put in the Entity
	so I can initialize the above variable 'player'
	
	Also, it's best to initialize any other variables you may have added,
	just like in any constructor.
	*/
	public ExtendedPlayer(EntityPlayer player)
	{
		this.player = player;
		// Start with max mana. Every player starts with the same amount.
		this.currentMana = this.maxMana = 50;
	}
	
	/**
	 * Used to register these extended properties for the player during EntityConstructing event
	 * This method is for convenience only; it will make your code look nicer
	 */
	public static final void register(EntityPlayer player)
	{
		player.registerExtendedProperties(ExtendedPlayer.EXT_PROP_NAME, new ExtendedPlayer(player));
	}
	
	/**
	 * Returns ExtendedPlayer properties for player
	 * This method is for convenience only; it will make your code look nicer
	 */
	public static final ExtendedPlayer get(EntityPlayer player)
	{
		return (ExtendedPlayer) player.getExtendedProperties(EXT_PROP_NAME);
	}
	
	// Save any custom data that needs saving here
	@Override
	public void saveNBTData(NBTTagCompound compound)
	{
		// We need to create a new tag compound that will save everything for our Extended Properties
		NBTTagCompound properties = new NBTTagCompound();
		
		// We only have 2 variables currently; save them both to the new tag
		properties.setInteger("CurrentMana", this.currentMana);
		properties.setInteger("MaxMana", this.maxMana);
		
		// Now add our custom tag to the player's tag with a unique name (our property's name)
		// This will allow you to save multiple types of properties and distinguish between them
		// If you only have one type, it isn't as important, but it will still avoid conflicts between
		// your tag names and vanilla tag names. For instance, if you add some "Items" tag,
		// that will conflict with vanilla. Not good. So just use a unique tag name.
		compound.setTag(EXT_PROP_NAME, properties);
		
	}

	// Load whatever data you saved
	@Override
	public void loadNBTData(NBTTagCompound compound)
	{
		// Here we fetch the unique tag compound we set for this class of Extended Properties
		NBTTagCompound properties = (NBTTagCompound) compound.getTag(EXT_PROP_NAME);
		// Get our data from the custom tag compound
		this.currentMana = properties.getInteger("CurrentMana");
		this.maxMana = properties.getInteger("MaxMana");
		// Just so you know it's working, add this line:
		System.out.println("[TUT PROPS] Mana from NBT: " + this.currentMana + "/" + this.maxMana);
	}
	
	/*
	I personally have yet to find a use for this method. If you know of any,
	please let me know and I'll add it in! 
	*/
	@Override
	public void init(Entity entity, World world)
	{
	}
	
	/*
	That's it for the IExtendedEntityProperties methods, but we need to add
	a few of our own in order to interact with our new variables. For now,
	let's make one method to consume mana and one to replenish it.
	 */
	
	/**
	 * Returns true if the amount of mana was consumed or false
	 * if the player's current mana was insufficient
	 */
	public boolean consumeMana(int amount)
	{
		// Does the player have enough mana?
		boolean sufficient = amount <= this.currentMana;
		// Consume the amount anyway; if it's more than the player's current mana,
		// mana will be set to 0
		this.currentMana -= (amount < this.currentMana ? amount : this.currentMana);
		// Return if the player had enough mana
		return sufficient;
	}
	
	/**
	 * Simple method sets current mana to max mana
	 */
	public void replenishMana()
	{
		this.currentMana = this.maxMana;
	}
}
/**
 * Step 2: Register the ExtendedPlayer class in your EventHandler
 */
/*
In order to access our newly created extended player properties, we need to
register them for every instance of EntityPlayer. That just means we're
creating a new instance of the class so we can access it.

Registering of IExtendedEntityProperties is all done in the EntityConstructing event.
*/
public class TutEventHandler
{
	@ForgeSubscribe
	public void onEntityConstructing(EntityConstructing event)
	{
		/*
		Be sure to check if the entity being constructed is the correct type
		for the extended properties you're about to add!
		The null check may not be necessary - I only use it to make sure
		properties are only registered once per entity
		*/
		if (event.entity instanceof EntityPlayer && ExtendedPlayer.get((EntityPlayer) event.entity) == null)
			// This is how extended properties are registered using our convenient method from earlier
			ExtendedPlayer.register((EntityPlayer) event.entity);
			// That will call the constructor as well as cause the init() method
			// to be called automatically
		
		// If you didn't make the two convenient methods from earlier, your code would be
		// much uglier:
		if (event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(ExtendedPlayer.EXT_PROP_NAME) == null)
			event.entity.registerExtendedProperties(ExtendedPlayer.EXT_PROP_NAME, new ExtendedPlayer((EntityPlayer) event.entity));
	}
}
/*
That's it! All players will now start with a pool of mana, so we just have to do
something with it.
*/
/**
 * Step 3.1: Using our new ExtendedPlayer Properties in an Item
 */
/*
For the sake of demonstration, we'll make a very basic item called... wait for it...
'ItemUseMana'. I should be naming mountains with this kind of stuff.

Anyways, here's our new ItemUseMana class. Since you probably know all about items
already, I will only discuss things related to IExtendedEntityProperties.
*/
public class ItemUseMana extends Item
{
	public ItemUseMana(int par1) {
		super(par1);
	}
	
	@Override
	public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer player)
    	{
		if (!world.isRemote)
		{
			/*
			Due to the length of code needed to get extended entity properties, I always find it
			handy to create a local variable named 'props' for whatever properties I need.
			
			Also, getExtendedProperties("name") returns the type 'IExtendedEntityProperties', so
			you need to cast it as your extended properties type for it to work.
			
			Old, ugly method:
			ExtendedPlayer props = (ExtendedPlayer) player.getExtendedProperties(ExtendedPlayer.EXT_PROP_NAME);
			
			This is using Seigneur_Necron's slick method (will be used from here on):
			 */
			ExtendedPlayer props = ExtendedPlayer.get(player);
			
			// Here we'll use the method we made to see if the player has enough mana to do something
			// We'll print something to the console for debugging, but I'm sure you'll find a much
			// better action to perform.
			if (props.consumeMana(15))
			{
				System.out.println("[MANA ITEM] Player had enough mana. Do something awesome!");
			}
			else
			{
				System.out.println("[MANA ITEM] Player ran out of mana. Sad face.");
				props.replenishMana();
			}
		}
		
        return itemstack;
    }
}
/*
Try it out and check the console. Hooray! It should have worked. If not, double-check
that you have registered your EventHandler in your main mod load method.

For the sake of convenience, here is the code:
*/
@EventHandler
public void load(FMLInitializationEvent event)
{
	MinecraftForge.EVENT_BUS.register(new TutEventHandler());
}
/**
 * Step 3.2: Using our new ExtendedPlayer Properties in an Event
 */
/*
Events are another really great place to use additional properties.
Since we only added mana, we will pretend that it is in fact a spell
that prevents damage from falling up to a certain distance.

In order to do this, we'll need to add a getCurrentMana method to
our ExtendedPlayer class. It just returns 'this.currentMana'

Next, we add a LivingFallEvent to our EventHandler:
*/
@ForgeSubscribe
public void onLivingFallEvent(LivingFallEvent event)
{
	// Remember that so far we have only added ExtendedPlayer properties
	// so check if it's the right kind of entity first
	if (event.entity instanceof EntityPlayer)
	{
		ExtendedPlayer props = ExtendedPlayer.get((EntityPlayer) event.entity);
		
		// This 'if' statement just saves a little processing time and
		// makes it so we only deplete mana from a fall that would injure the player
		if (event.distance > 3.0F && props.getCurrentMana() > 0)
		{
			// Some debugging statements so you can see what's happening
			System.out.println("[EVENT] Fall distance: " + event.distance);
			System.out.println("[EVENT] Current mana: " + props.getCurrentMana());
			
			/*
			We need to make a local variable to store the amount to reduce both
			the distance and mana, otherwise when we reduce one, we have no way
			to tell by how much to reduce the other
			
			Alternatively, you could just try to consumeMana for the amount of the
			fall distance and, if it returns true, set the fall distance to 0,
			but today we're going for a cushioning effect instead.

			If you want mana to be used efficiently, you would only reduce the fall
			distance by enough to reduce it to 3.0F (3 blocks), thus ensuring the
			player will take no damage while minimizing mana consumed.
				
			Be sure you put (event.distance - 3.0F) in parentheses or you'll have a
			nasty bug with your mana! It has to do with the way "x < y ? a : b"
			parses parameters.
			*/
			float reduceby = props.getCurrentMana() < (event.distance - 3.0F) ? props.getCurrentMana() : (event.distance - 3.0F);
			event.distance -= reduceby;
			
			// Cast reduceby to 'int' to match our method parameter
			props.consumeMana((int) reduceby);
			
			System.out.println("[EVENT] Adjusted fall distance: " + event.distance);
		}
	}
}
/**
 * Step 3.3: Using our ExtendedPlayer Properties in a Gui Overlay
 */
/*
Here we will create a mana bar display in the upper-left corner of the screen using
currentMana and maxMana from our ExtendedPlayer class.

The first part of this section is from http://www.minecraftforge.net/wiki/Gui_Overlay
I highly recommend you read that tutorial before continuing on, as it contains lots
of great information related to this topic.
*/
@SideOnly(Side.CLIENT)
public class GuiManaBar extends Gui
{
	private Minecraft mc;
	/* (my added notes:)
	ResourceLocation takes 2 arguments: your mod id and the path to your texture file,
	starting from the folder 'textures/' from '/src/minecraft/assets/yourmodid/'
	
	The texture file must be 256x256 (or multiples thereof)
	
	I have provided a functional (but ugly) mana_bar.png file to use with this tutorial.
	Download it from Forge_Tutorials/textures/gui
	 */
	private static final ResourceLocation texturepath = new ResourceLocation("tutorial", "textures/gui/mana_bar.png");

	public GuiManaBar(Minecraft mc)
	{
		super();
		// We need this to invoke the render engine.
		this.mc = mc;
	}

	//
	// This event is called by GuiIngameForge during each frame by
	// GuiIngameForge.pre() and GuiIngameForce.post().
	//
	@ForgeSubscribe(priority = EventPriority.NORMAL)
	public void onRenderExperienceBar(RenderGameOverlayEvent event)
	{
		// We draw after the ExperienceBar has drawn.  The event raised by GuiIngameForge.pre()
		// will return true from isCancelable.  If you call event.setCanceled(true) in
		// that case, the portion of rendering which this event represents will be canceled.
		// We want to draw *after* the experience bar is drawn, so we make sure isCancelable() returns
		// false and that the eventType represents the ExperienceBar event.
		if (event.isCancelable() || event.type != ElementType.EXPERIENCE)
		{
			return;
		}
		
		/** Start of my tutorial */
		
		// Get our extended player properties and assign it locally so we can easily access it
		ExtendedPlayer props = ExtendedPlayer.get(this.mc.thePlayer);
		
		// If for some reason these properties don't exist (perhaps in multiplayer?)
		// or the player doesn't have mana, return. Note that I added a new method
		// 'getMaxMana()' to ExtendedPlayer for this purpose
		if (props == null || props.getMaxMana() == 0)
		{
			return;
		}

		// Starting position for the mana bar - 2 pixels from the top left corner.
		int xPos = 2;
		int yPos = 2;
		
		// The center of the screen can be gotten like this during this event:
		// int xPos = event.resolution.getScaledWidth() / 2;
		// int yPos = event.resolution.getScaledHeight() / 2;

		// Be sure to offset based on your texture size or your texture will not be truly centered:
		// int xPos = (event.resolution.getScaledWidth() + textureWidth) / 2;
		// int yPos = (event.resolution.getScaledHeight() + textureHeight) / 2;
		
		// setting all color values to 1.0F will render the texture as it appears in your texture file
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		// Somewhere in Minecraft vanilla code it says to do this because of a lighting bug
		GL11.glDisable(GL11.GL_LIGHTING);
		
		// Bind your texture to the render engine
		this.mc.getTextureManager().bindTexture(texturepath);
		
		/*
		The parameters for drawTexturedModalRect are as follows:
		
		drawTexturedModalRect(int x, int y, int u, int v, int width, int height);
		
		x and y are the on-screen position at which to render.
		u and v are the coordinates of the most upper-left pixel in your texture file from which to start drawing.
		width and height are how many pixels to render from the start point (u, v)
		 */
		// First draw the background layer. In my texture file, it starts at the upper-
		// left corner (x=0, y=0), is 50 pixels long and 4 pixels thick (y value)
		this.drawTexturedModalRect(xPos, yPos, 0, 0, 50, 4);
		// Then draw the foreground; it's located just below the background in my
		// texture file, so it starts at x=0, y=4, is only 2 pixels thick and 50 length
		// Why y=4 and not y=5? Y starts at 0, so 0,1,2,3 = 4 pixels for the background
		
		// However, we want the length to be based on current mana, so we need a new variable:
		int manabarwidth = (int)(((float) props.getCurrentMana() / props.getMaxMana()) * 50));
		System.out.println("[GUI MANA] Current mana bar width: " + manabarwidth);
		// Now we can draw our mana bar at yPos+1 so it centers in the background:
		this.drawTexturedModalRect(xPos, yPos + 1, 0, 4, manabarwidth, 2);
	}
}
/*
If you want to make your mana bar look really sweet with some transparency like the hotbar slots, then you'll have
to add a little extra code and make sure your texture is set up correctly. An easy way to do that is to open the
minecraft jar in 7-zip and extract the texture containing the hotbar (called 'widgets'), then copy the transparent
part of the texture into your file.

Note if you use the following code, you'll need to use 'mana_bar2.png' instead.
*/
// Add this block of code before you draw the section of your texture containing transparency
GL11.glEnable(GL11.GL_BLEND);
GL11.glDisable(GL11.GL_DEPTH_TEST);
GL11.glDepthMask(false);
GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
GL11.glDisable(GL11.GL_ALPHA_TEST);
// Here we draw the background bar which contains a transparent section; note the new size
drawTexturedModalRect(xPos, yPos, 0, 0, 56, 9);
// You can keep drawing without changing anything, but see the following note!
int manabarwidth = (int)(((float) props.getCurrentMana() / props.getMaxMana()) * 49);
drawTexturedModalRect(xPos + 3, yPos + 3, 0, 9, manabarwidth, 3);
// NOTE: be sure to reset the openGL settings after you're done or your character model will be messed up
GL11.glEnable(GL11.GL_DEPTH_TEST);
GL11.glDepthMask(true);
/*
Now when you use up your mana, the background bar will be semi-transparent. Cool!

You will need to add this code to your main mod class postInit method in order to register your new GuiManaBar
overlay as an active event (just like registering your EventHandler), otherwise nothing will appear. Recall that
the Gui is client side only, so you will get an error if you try to register it on both sides.
*/
@EventHandler
public void postInit(FMLPostInitializationEvent event)
{
if (FMLCommonHandler.instance().getEffectiveSide().isClient())
MinecraftForge.EVENT_BUS.register(new GuiManaBar(Minecraft.getMinecraft()));
}
/*
Alright, try it out. There should be a horizontal mana bar in the upper-left corner of 
your screen. Now use our ItemUseMana once or twice. Notice the mana bar doesn't update.

What's going on?

Well, currently only the server knows how much mana the player has. We never told the client
that the player even has mana, let alone how much! For many purposes, this is fine. However,
since a Gui is only rendered client side, this is a case where we will need to use packets
to synchronize the server/client.

Please take a few minutes to read up on Packet Handling, as I'm not going to cover it in
much detail here:
http://www.minecraftforge.net/wiki/Tutorials/Packet_Handling

Ok, moving on.
Make a packet handler class like in the above tutorial:
*/
public class TutorialPacketHandler implements IPacketHandler
{
	// Don't need to do anything here.
	public TutorialPacketHandler() {}

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
	{
		// This is a good place to parse through channels if you have multiple channels
		if (packet.channel.equals("tutchannel")) {
			handleExtendedProperties(packet, player);
		}
	}
	
	// Making different methods to handle each channel helps keep things tidy:
	private void handleExtendedProperties(Packet250CustomPayload packet, Player player)
	{
		DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(packet.data));
		
		ExtendedPlayer props = ExtendedPlayer.get((EntityPlayer) player);

		// Everything we read here should match EXACTLY the order in which we wrote it
		// to the output stream in our ExtendedPlayer sync() method.
		try {
			props.setMaxMana(inputStream.readInt());
			props.setCurrentMana(inputStream.readInt());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		// Just so you can see in the console that it's working:
		System.out.println("[PACKET] Mana from packet: " + props.getCurrentMana() + "/" + props.getMaxMana());
	}
}
/*
Then modify the following line in your main mod class to the below:
*/
@NetworkMod(clientSideRequired=true, serverSideRequired=false, channels = {"tutchannel"}, packetHandler = TutorialPacketHandler.class)
/*
That's it for setting up the Packet Handler framework, now we'll set up a method to send
packets from within our ExtendedPlayer class. I like to give this method the same name
in all of my IExtendedEntityProperties classes, just to make it easy on myself.
*/
/**
 * Sends a packet to the client containing information stored on the server
 * for ExtendedPlayer
 */
public final void sync()
{
	ByteArrayOutputStream bos = new ByteArrayOutputStream(8);
	DataOutputStream outputStream = new DataOutputStream(bos);
	
	// We'll write max mana first so when we set current mana client
	// side, it doesn't get set to 0 (see methods below)
	try {
		outputStream.writeInt(this.maxMana);
		outputStream.writeInt(this.currentMana);
	} catch (Exception ex) {
		ex.printStackTrace();
	}

	Packet250CustomPayload packet = new Packet250CustomPayload("tutchannel", bos.toByteArray);
	
	// We only want to send from the server to the client
	if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
		EntityPlayerMP player1 = (EntityPlayerMP) player;
		PacketDispatcher.sendPacketToPlayer(packet, (Player) player1);
	}
}
/*
Okay, well that will send a packet whenever we call it. Problem is, we don't call it
anywhere yet. You could do it in onLivingUpdate or some such, but that would unnecessarily
spam packets which would be no good. We only want to call it when any of the information
stored in ExtendedPlayer changes, in our case, when current or max mana is modified.

Here you can see my implementations for setCurrentMana and setMaxMana:
 */
/**
 * Sets current mana to amount or maxMana, whichever is lesser
 */
public void setCurrentMana(int amount)
{
	this.currentMana = (amount < this.maxMana ? amount : this.maxMana);
	this.sync();
}

/**
 * Sets max mana to amount or 0 if amount is less than 0
 */
public void setMaxMana(int amount)
{
	this.maxMana = (amount > 0 ? amount : 0);
	this.sync();
}
/*
Note that we add a call to sync() our ExtendedProperties in each of these methods because they
changed our stored variables. We also need to sync the properties in any other methods that
do so, like consumeMana and replenishMana.

Another time we need to sync properties is after the entity is loaded from NBT, as that is
only done server side and we want the information for our GuiManaBar. Because we can't
send and receive packets before everything is loaded, we can't do it from within the
readFromNBT method. Guess where we can do it from? That's right, our EventHandler!

Add this to your EventHandler's onEntityJoinWorldEvent method, as this event occurs
after everything (the world, entities, etc) is loaded but before anything really happens
in the game. As a bonus, it's only called once per entity, so you're not spamming packets.
*/
@ForgeSubscribe
public void onEntityJoinWorld(EntityJoinWorldEvent event)
{
	//Only need to synchronize when the world is remote (i.e. we're on the server side)
	// and only for player entities, as that's what we need for the GuiManaBar
	if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer)
		ExtendedPlayer.get((EntityPlayer) event.entity).sync();
}
/*
Anyways, that's a lot of work just to get a little mana bar, but it should all be working
correctly now. Fire it up and try for yourself!
*/
/**
 * Step 3.4: Using DataWatcher to Synchronize
 */
/*
[SPOILER]
Minecraft has a built-in functionality specifically to keep certain kinds of variables synchronized between server
and client that is highly suited to our needs. It's called 'DataWatcher'. Please head over to the Minecraft Forge
Wiki and read this tutorial first, then come back.

Okay, simple enough. DataWatcher is best used for things that are constantly fluctuating, such as health, hunger, or,
in our case, current mana. Using DataWatcher instead of packets has the advantage of... we don't have to use packets!
But only for the variable we watch. Everything else will still need to use packets, but now we can send packets much
less frequently.

To use DataWatcher, first we need to add a new watchable object to the player. We can do this in the constructor of
our ExtendedPlayer properties. First, I will define the index to use, in case we need to change it later due to
unforseen conflicts with other DataWatcher objects (if you read the earlier tutorial, you'll know there are only 32
available slots, so this is an important step to save yourself hassle later).
*/
/** This will be the index of our watchable object in DataWatcher */
public static final int MANA_WATCHER = 20;

public ExtendedPlayer(EntityPlayer player)
{
	this.player = player;
	this.maxMana = 50;

	// This adds the new object at our defined index and sets the value to max mana,
	// since we should have full mana when first constructing
	this.player.getDataWatcher().addObject(MANA_WATCHER, this.maxMana);
}
/*

Notice that we no longer set the variable 'currentMana'? That's because it is now stored in DataWatcher, so we no
longer need it. Delete that variable from the class. Errors will show up - these are all the places we need to change.

First we'll fix NBT saving and loading.
*/
// To save, we simply replace 'currentMana' with getWatchableObjectInt
properties.setInteger("CurrentMana", this.player.getDataWatcher().getWatchableObjectInt(MANA_WATCHER));

// To load, we need to use 'updateObject' NOT 'addObject', as the object is already added.
// If you try to add it again, you will get an error
this.player.getDataWatcher().updateObject(MANA_WATCHER, properties.getInteger("CurrentMana"));

// Next we need to fix all the methods that deal with changing currentMana:

// This method gets a little messier, unfortunately, due to the unwieldy length of getting information
// from DataWatcher vs. referencing a local variable, so we'll create a local variable instead
public final boolean consumeMana(int amount)
{
	// This variable makes it easier to write the rest of the method
	int mana = this.player.getDataWatcher().getWatchableObjectInt(MANA_WATCHER);

	// These two lines are the same as before
	boolean sufficient = amount <= mana;
	mana -= (amount < mana ? amount : mana);

	// Update the data watcher object with the new value
	this.player.getDataWatcher().updateObject(MANA_WATCHER, mana);

	// note that we no longer need to call 'sync()' to update the client

	return sufficient;
}

// This method cleans up nicely - no more call to 'sync()' means no custom packet to handle!
public final void replenishMana()
{
this.player.getDataWatcher().updateObject(MANA_WATCHER, this.maxMana);
}

// Simple change
public final int getCurrentMana()
{
return this.player.getDataWatcher().getWatchableObjectInt(MANA_WATCHER);
}

// Again, use 'updateObject'; we don't need to 'sync()' here anymore
public final void setCurrentMana(int amount)
{
this.player.getDataWatcher().updateObject(MANA_WATCHER, (amount < this.maxMana ? amount : this.maxMana));
}
/*
Now remove 'currentMana' from your sync() method and packet handler, but keep in mind you still need to synchronize
'maxMana' with a packet, so you'll need to keep the sync() method. Note that the packet size will now be '4' if you've
followed along exactly.

Why not use DataWatcher for maxMana too? Well you could, but since it rarely changes, it would be a waste of such a
limited resource.

That's pretty much it. Now current mana will stay in sync for the gui display automatically and you'll send far fewer
packets. If you ever run into a conflict with another watchable object using the same index '20', you only need to
change the value of MANA_WATCHER to update every instance in your code.
*/
/**
 * Step 4: Adding another kind of Extended Properties
 */
/*
Now we're going to add a variable to all EntityLivingBase entities in addition
to the ones we added above for EntityPlayer. We only want players to have mana,
but we want every creature under the sun to have riches for us to plunder!

Well, guess what it will be named? That's right, ExtendedLiving! this class, as
it will be almost exactly like the one we just made, but with one difference:
we're going to use the init() method so we can use the Random from World object to
randomize the amount of gold each entitylivingbase has.
*/
public class ExtendedLiving implements IExtendedEntityProperties
{
	public final static String EXT_PROP_NAME = "ExtendedLiving";
	
	private final EntityLivingBase entity;
	
	private int gold;

	public ExtendedLiving(EntityLivingBase entity)
	{
		this.entity = entity;
	}
	
	/**
	 * Used to register these extended properties for the entity during EntityConstructing event
	 * This method is for convenience only; it will make your code look nicer
	 */
	public static final void register(EntityLivingBase entity)
	{
		entity.registerExtendedProperties(ExtendedLiving.EXT_PROP_NAME, new ExtendedLiving(entity));
	}
	
	/**
	 * Returns ExtendedLiving properties for entity
	 * This method is for convenience only; it will make your code look nicer
	 */
	public static final ExtendedLiving get(EntityLivingBase entity)
	{
		return (ExtendedLiving) entity.getExtendedProperties(EXT_PROP_NAME);
	}

	@Override
	public void saveNBTData(NBTTagCompound compound)
	{
		compound.setInteger("Gold", this.gold);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound)
	{
		this.gold = compound.getInteger("Gold");
		System.out.println("[LIVING BASE] Gold from NBT: " + this.gold);
	}

	@Override
	public void init(Entity entity, World world)
	{
		// Gives a random amount of gold between 0 and 15
		this.gold = world.rand.nextInt(16);
		System.out.println("[LIVING BASE] Gold: " + this.gold);
	}
}
// Be sure to register it in your EventHandler!
// onEntityConstructing should now look like this:
@ForgeSubscribe
public void onEntityConstructing(EntityConstructing event)
{
	// From last time:
	if (event.entity instanceof EntityPlayer && ExtendedPlayer.get((EntityPlayer) event.entity) == null)
		ExtendedPlayer.register((EntityPlayer) event.entity);
	/* New stuff:
	Be sure not to use 'else if' here. A player, for example, is both an EntityPlayer
	AND an EntityLivingBase, so should have both extended properties
	*/ 
	if (event.entity instanceof EntityLivingBase)
		/*
		Just like above but we change 'ExtendedPlayer' to 'ExtendedLivingBase'
		and cast event.entity to EntityLivingBase.
		Isn't it nice to use the constant variable EXT_PROP_NAME in all
		of our IExtendedEntityProperty classes? So easy to remember and it
		stores a different name for each class. Nice.
		*/
		ExtendedLiving.register((EntityLivingBase) event.entity);
		
		/* Old, cumbersome method:
		event.entity.registerExtendedProperties(ExtendedLivingBase.EXT_PROP_NAME,
				new ExtendedLivingBase((EntityLivingBase) event.entity));
		*/
		// Remember, this will also call the init() method automatically
}
/*
Finished! Go ahead and give it a try, see how much gold everyone is getting!

Wait, what the heck?! What's with all these errors, you ask? Well, to be honest,
I'm not really sure. Something to do with the way the init() method is called leads
to the ExtendedProperties being null somehow. No idea.

Good news is, I know how to get around it. Move everything that seems like it should
go in init() to your EventHandler onEntityJoinWorldEvent. Be sure to leave the init
method totally empty.
*/
@ForgeSubscribe
public void onEntityJoinWorld(EntityJoinWorldEvent event)
{
	if (event.entity instanceof EntityLivingBase)
	{
		ExtendedLiving props = ExtendedLiving.get((EntityLivingBase) event.entity);
		// Gives a random amount of gold between 0 and 15
		props.addGold(event.entity.worldObj.rand.nextInt(16));
		System.out.println("[LIVING BASE] Gold: " + props.getGold());
	}
}
/*
Be sure to create the 'addGold(int)' and 'getGold()' methods in ExtendedLivingBase.
Now try it. Yay! It works!

In conjunction with a custom ItemGoldCoin, LivingDropsEvent and EntityItemPickupEvent, I'm sure you can see how
this could be used to store large amounts of coin without clogging up the inventory.
*/
/**
 * Step 5: Getting your custom data to persist through player death
 */
/*
First, many thanks and godly praise upon the legend that is Mithion, creator of IExtendedEntityProperties.
Without him, not only this particular section, but all of this stuff would not be possible. Truly amazing work
by him that allows us to do so much with ease.

Just had to be said. Anyways, as some have noticed, if you die and respawn, the data you so carefully created,
registered, sent in packets and saved to NBT is RESET to the initial values when the player dies. This has to do
with the way player NBT is stored and retrieved during death, and there's nothing we can do about it. At least,
nothing directly.

We need to find a way to store the IExtendedEntityProperties data outside of the player when the player dies, and
retrieve it when the player respawns. I knew this, but I never would have thought of where to store it without
Mithion's assistance. Though I guess it could be stored anywhere that persists...

Also thanks to Seigneur_Necron for some excellent pointers on good coding practice. This helped clean

up aspects of the code and improve the overall quality. Merci!

Enough blabbing. The most convenient way to store extended properties is bundled as NBT, and we'll be storing it in a
HashMap with the player's username as the key. We'll store this map in our CommonProxy class.
*/
public class CommonProxy implements IGuiHandler
{
	/** Used to store IExtendedEntityProperties data temporarily between player death and respawn */
	private static final Map<String, NBTTagCompound> extendedEntityData = new HashMap<String, NBTTagCompound>();

	public void registerRenderers() {}

	@Override
	public Object getServerGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}

	@Override
	public Object getClientGuiElement(int guiId, EntityPlayer player, World world, int x, int y, int z)
	{
		return null;
	}

	/**
	* Adds an entity's custom data to the map for temporary storage
	* @param compound An NBT Tag Compound that stores the IExtendedEntityProperties data only
	*/
	public static void storeEntityData(String name, NBTTagCompound compound)
	{
		extendedEntityData.put(name, compound);
	}

	/**
	* Removes the compound from the map and returns the NBT tag stored for name or null if none exists
	*/
	public static NBTTagCompound getEntityData(String name)
	{
		return extendedEntityData.remove(name);
	}
}
/*
Ok, now we have the framework up that will store our data externally to the player's NBT, which we can access
from our EventHandler. Now all we need to do is store the data when the player dies and retrieve it when the
player re-joins the world.

We don't use LivingSpawnEvent because that event is not triggered during the process of dying and being respawned,
as much as you would think otherwise.
*/
// These are methods in the EventHandler class, in case you don't know that by now

// we need to add this new event - it is called for every living entity upon death
@ForgeSubscribe
public void onLivingDeathEvent(LivingDeathEvent event)
{
	// we only want to save data for players (most likely, anyway)
	if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer)
	{
		// NOTE: See step 6 for a way to do this all in one line!!!
		
		// create a new NBT Tag Compound to store the IExtendedEntityProperties data
		NBTTagCompound playerData = new NBTTagCompound();
		// write the data to the new compound
		ExtendedPlayer.get((EntityPlayer) event.entity).saveNBTData(playerData);
		// and store it in our proxy
		proxy.storeEntityData(((EntityPlayer) event.entity).username, playerData);
	}
}

// we already have this event, but we need to modify it some
@ForgeSubscribe
public void onEntityJoinWorld(EntityJoinWorldEvent event)
{
	if (!event.entity.worldObj.isRemote && event.entity instanceof EntityPlayer)
	{
		// NOTE: See step 6 for a way to do this all in one line!!!
		
		// before syncing the properties, we must first check if the player has some saved in the proxy
		// recall that 'getEntityData' also removes it from the map, so be sure to store it locally
		NBTTagCompound playerData = proxy.getEntityData(((EntityPlayer) event.entity).username);
		// make sure the compound isn't null
		if (playerData != null)
		{
			// then load the data back into the player's IExtendedEntityProperties
			ExtendedPlayer.get((EntityPlayer) event.entity).loadNBTData(playerData);
		}
		// finally, we sync the data between server and client (we did this earlier in 3.3)
		ExtendedPlayer.get((EntityPlayer) event.entity).sync();
	}
}
/*
But wait, didn't we add TWO extended properties? How can a single entity store two different properties?

Easy, append the properties name to the username when storing and retrieving. That way, each property is
stored with its own unique name and the player it belongs to.
*/
// save player data:
proxy.storeEntityData(((EntityPlayer) event.entity).username + ExtendedPlayer.EXT_PROP_NAME, playerData);
// save living data:
proxy.storeEntityData(((EntityPlayer) event.entity).username + ExtendedLiving.EXT_PROP_NAME, playerData);

// load player data:
NBTTagCompound playerData = proxy.getEntityData(((EntityPlayer) event.entity).username + ExtendedPlayer.EXT_PROP_NAME);
// load living data
NBTTagCompound playerData = proxy.getEntityData(((EntityPlayer) event.entity).username + ExtendedLiving.EXT_PROP_NAME);
/*
Note that this is still saving data ONLY for player entities, but a player with multiple types of Extended Properties.

This will also allow inter-mod compatibility if, for example, two mods both add custom data to the player using
IExtendedEntityProperties AND you've given your properties a unique name (NOT something like "ExtendedPlayer", which
I used in the tutorial).
*/
/**
 * Step 6: Improvements to the code, courtesy of Seigneur_Necron
 */
/*
Some of these I incorporated into the above tutorial, but there are more that I didn't. Here are some really
great tips from Seigneur_Necron.
*/
/**
 * 6.1: Create a simple method that returns your ExtendedProperties
 */
public static ExtendedPlayer get(EntityPlayer player)
{
	return (ExtendedPlayer) player.getExtendedProperties(EXT_PROP_NAME);
}
// Here's how it looks in use. I know which one I prefer ;)

// Seigneur_Necron's elegance:
ExtendedPlayer props = ExtendedPlayer.get(player);

// vs my original super-cumbersome method:
ExtendedPlayer props = (ExtendedPlayer) event.entity.getExtendedProperties(ExtendedPlayer.EXT_PROP_NAME);
/**
 * 6.2: Create methods to save / load ExtendedProperty data to your CommonProxy
 */
/**
 * Makes it look nicer in the methods save/loadProxyData
 */
private static String getSaveKey(EntityPlayer player) {
	return player.username + ":" + EXT_PROP_NAME;
}

/**
 * Does everything I did in onLivingDeathEvent and it's static,
 * so you now only need to use the following in the above event:
 * ExtendedPlayer.saveProxyData((EntityPlayer) event.entity));
 */
public static void saveProxyData(EntityPlayer player)
{
	ExtendedPlayer playerData = ExtendedPlayer.get(player);
	NBTTagCompound savedData = new NBTTagCompound();

	playerData.saveNBTData(savedData);
	// Note that we made the CommonProxy method storeEntityData static,
	// so now we don't need an instance of CommonProxy to use it! Great!
	CommonProxy.storeEntityData(getSaveKey(player), savedData);
}

/**
 * This cleans up the onEntityJoinWorld event by replacing most of the code
 * with a single line: ExtendedPlayer.loadProxyData((EntityPlayer) event.entity));
 */
public static void loadProxyData(EntityPlayer player)
{
	ExtendedPlayer playerData = ExtendedPlayer.get(player);
	NBTTagCompound savedData = CommonProxy.getEntityData(getSaveKey(player));

	if(savedData != null) {
		playerData.loadNBTData(savedData);
	}
	
	// note we renamed 'syncExtendedProperties' to 'syncProperties' because yay, it's shorter
	playerData.syncProperties();
}
/**
 * 6.3: Create a 'register' method to clean up EntityConstructing code:
 */
/**
 * Used to register these extended properties for the player during EntityConstructing event
 */
// Note that it's static, so we can call it like this in the event:
// ExtendedPlayer.register((EntityPlayer) event.entity);
public static void register(EntityPlayer player)
{
	player.registerExtendedProperties(ExtendedPlayer.EXT_PROP_NAME, new ExtendedPlayer(player));
}
/**
 * 6.4: Fancy packet handling stuff that is a bit beyond me, but I'll put it here for completeness.
 */
public class StargatePacketHandler implements IPacketHandler
{

@Override
public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player)
{
	if(packet != null && packet.channel != null && packet.data != null)
	{
		if(packet.channel.equals(StargateMod.CHANEL_TILE_ENTITY) && packet.length >= 20) {
			this.handleTileEntityPacket(packet, (EntityPlayer) player);
		}
		else if(packet.channel.equals(StargateMod.CHANEL_COMMANDS) && packet.length >= 20) {
			this.handleCommandPacket(packet, (EntityPlayer) player);
		}
		else if(packet.channel.equals(StargateMod.CHANEL_PLAYER_DATA) && packet.length >= 4) {
			this.handlePlayerDataPacket(packet, (EntityPlayer) player);
		}
	}
}

protected void handlePlayerDataPacket(Packet250CustomPayload packet, EntityPlayer player)
{
	DataInputStream input = new DataInputStream(new ByteArrayInputStream(packet.data));

	try {
		int id = input.readInt();
		Class<? extends PlayerDataList> clazz = getClassFromPlayerDataPacketId(id);

		PlayerDataList playerData = null;

		if(clazz == PlayerTeleporterData.class) {
			playerData = PlayerTeleporterData.get(player);
		}
		else if(clazz == PlayerStargateData.class) {
			playerData = PlayerStargateData.get(player);
		}

		if(playerData != null) {
			playerData.loadProperties(input);
		}

		input.close();
	}
	catch(IOException argh) {
		StargateMod.debug("Error while reading custom player data from DataInputStream.", Level.SEVERE, true);
		argh.printStackTrace();
		return;
	}
}

// Other stuff...

}
/*
Thanks again to Seigneur_Necron for these great tips!

And that's how it's done, folks. Happy modding. :D
*/
