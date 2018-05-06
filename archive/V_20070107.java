/**
* Vector Racer 4K 
* @author Harm Boschloo
*/

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;
import javax.swing.JFrame;
import java.util.Calendar;

/**
 * @author Harm Boschloo
 */
class V extends JFrame
{

	private static final int WIDTH = 630;
	private static final int HEIGHT = 480;
	private static final int CP_R = 25;
	private static final int MIN_CP = 3;
	private static final int MAX_CP = 5;
	//private static final int TRACK_SIZE = 50;
	private static final int GRID_SIZE = 15;
	
	//private static final Color TRACK_COLOR = Color.WHITE;
	//private static final Color WALL_COLOR = new Color( 250,250,250 );
	//private static final Color WALL_OUTLINE_COLOR = new Color( 60,120,240 );
	private static final Color GRID_COLOR = new Color( 195,210,240,120 );
	//private static final Color CP_TEXT_COLOR = new Color( 60,120,240,200 );

	private static final int MAX_PLAYERS = 9;
	private static final int PLAYER_R = 4;
    private static final Color[] PLAYER_COLORS = {
        new Color( 255,0,0 ),
        new Color( 255,160,0 ),
        new Color( 0,200,0 ),
        new Color( 0,0,200 ),
        new Color( 180,0,0 ),
        new Color( 120,0,200 ),
        new Color( 200,0,160 ),
        new Color( 0,160,200 ),
        new Color( 0,200,150 )
    };

	private static final int MENU_STATE = 0;
	//private static final int INIT_STATE = 1;
	private static final int INPUT_STATE = 2;
	private static final int PROCESS_STATE = 3;
	private static final int SIM_STATE = 4;

	private int state = MENU_STATE;
	private BufferedImage bg = new BufferedImage( WIDTH+1,HEIGHT+1,BufferedImage.TYPE_INT_ARGB );
	private BufferedImage mask = new BufferedImage( WIDTH+1,HEIGHT+1,BufferedImage.TYPE_INT_ARGB );
	private int[] p_x = new int[MAX_PLAYERS];
	private int[] p_y = new int[MAX_PLAYERS];
	private int[] p_x0 = new int[MAX_PLAYERS];
	private int[] p_y0 = new int[MAX_PLAYERS];
	private int[] p_u = new int[MAX_PLAYERS];
	private int[] p_v = new int[MAX_PLAYERS];
	private int[] p_target = new int[MAX_PLAYERS];
	private int[] p_i = new int[MAX_PLAYERS];
	private int[] cp_x = new int[MAX_CP];
	private int[] cp_y = new int[MAX_CP];
	private int cp_n = 0;
	private int du = 0;
	private int dv = 0;
	private int currentPlayer = 0;
	private long t = 0;
	private long t0 = 0;
	private long tm0 = 0;
	private String scoreboard = "";
	
	/**
	 * Entry point.
	 * @param argv The arguments passed in to the program.
	 */
	public static void main(String argv[])
	{
		new V();
	}

	
	private V()
	{
		super("VR4K");
		
		setSize( WIDTH+20,HEIGHT+50 );
		//setResizable( false );
		show();
		
		createBufferStrategy(2);
		BufferStrategy strategy = getBufferStrategy();

		newCircuit();
		
		// Draw loop
		while( true ) {
			draw( (Graphics2D)strategy.getDrawGraphics() );
			strategy.show();
			if ( !isVisible() ) {
				System.exit( 0 );
			}
			
			if ( state == SIM_STATE ) {
				simulateStep();
			}
			
			try {
				Thread.sleep( 10 );
			}
			catch( Exception e ) {
			}
		}
	}
		
	/**
	* @param e the key event
	*/
	protected void processKeyEvent( KeyEvent e )
	{
		if ( e.getID() == KeyEvent.KEY_PRESSED ) {
			if ( state == INPUT_STATE ) {
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD1 ) {
					du = -GRID_SIZE;
					dv = GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD2 ) {
					du = 0;
					dv = GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD3 ) {
					du = GRID_SIZE;
					dv = GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD4 ) {
					du = -GRID_SIZE;
					dv = 0;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD5 ) {
					du = 0;
					dv = 0;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD6 ) {
					du = GRID_SIZE;
					dv = 0;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD7 ) {
					du = -GRID_SIZE;
					dv = -GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD8 ) {
					du = 0;
					dv = -GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_NUMPAD9 ) {
					du = GRID_SIZE;
					dv = -GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					state = PROCESS_STATE;
					p_u[currentPlayer] += du;
					p_v[currentPlayer] += dv;
					
					du = 0;
					dv = 0;
					
					// next player
					while( true ) {
						currentPlayer++;
						if ( p_x.length == currentPlayer ) {
							currentPlayer = 0; //[todo check of no players in race]
							initSimulateStep();
							state = SIM_STATE;
							break;
						}
						else if ( p_x[currentPlayer] != 0 ) {
							state = INPUT_STATE;
							break;
						}
					}
					
				}
			}	
			
			if ( state == MENU_STATE )
			{
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					state = PROCESS_STATE;
					newCircuit();
					newRace( 2 );
					state = INPUT_STATE;
				}
				
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
				{
					System.exit( 0 );
				}
			}
			
			if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
			{
				state = MENU_STATE;
			}
		}
	}
	
	/** 
	*  @param g The graphics context in which to paint
	*/
	private void draw( Graphics2D g )
	{
		g.translate(10,35);	
		
		// background
		g.drawImage( bg, 0, 0, null );
		
		// target cp
		if ( p_x[currentPlayer] != 0 ) {
			g.setColor( PLAYER_COLORS[currentPlayer] );
			g.drawOval( cp_x[p_target[currentPlayer]]-CP_R,cp_y[p_target[currentPlayer]]-CP_R,2*CP_R,2*CP_R );
		}
		
		// players
		for ( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( p_x[i] != 0 ) {
				g.setColor( PLAYER_COLORS[i] );
				g.drawOval( p_x[i]-PLAYER_R,p_y[i]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
			}
		}
		
		// scoreboard
		g.drawString( scoreboard,400,20 );
		
		// input selection
		if ( state == INPUT_STATE ) {
			g.setColor( PLAYER_COLORS[currentPlayer] );
			g.drawLine( 
				p_x[currentPlayer],
				p_y[currentPlayer],
				p_x[currentPlayer]+p_u[currentPlayer]+du,
				p_y[currentPlayer]+p_v[currentPlayer]+dv );
			
			if ( du != 0 && dv != 0 ) {
				//draw acceleration triangle
				//...
			}
		}
		
		// main menu
		if ( state == MENU_STATE ) {
			g.setColor( Color.BLACK );
			g.drawString( "Enter - start new game",50,100 );
		}

		// debug
		g.drawString( "t  " + String.valueOf( t/1000.0 ),10,20 );
		g.drawString( "p  " + String.valueOf( currentPlayer ),10,40 );

	}
	
	/**
	 *  new race.
	 */
	private void newCircuit()
	{
		// create checkpoints
		cp_n = random( MIN_CP, MAX_CP );
		outer:for ( int i = 0; i < cp_n; ) {
			cp_x[i] = random( GRID_SIZE+CP_R, WIDTH-GRID_SIZE-CP_R );
			cp_y[i] = random( GRID_SIZE+CP_R, HEIGHT-GRID_SIZE-CP_R );
			for ( int ii = 0; ii < i; ii++ ) {
				if ( Math.sqrt( Math.pow( cp_x[i]-cp_x[ii],2 ) + Math.pow( cp_y[i]-cp_y[ii],2 ) ) < 2*CP_R + 2*GRID_SIZE ) {
					continue outer;
				}
			}
			i++;
		}
	
		// create obstacles
		int ob_n = random( 15, 25 );
		int ob_r = 30;
		int[] ob_x = new int[ob_n];
		int[] ob_y = new int[ob_n];
		outer:for ( int i = 0; i < ob_n; ) {
			ob_x[i] = random( 0, WIDTH );
			ob_y[i] = random( 0, HEIGHT );
			for ( int ii = 0; ii < i; ii++ ) {
				if ( Math.sqrt( Math.pow( ob_x[i]-ob_x[ii],2 ) + Math.pow( ob_y[i]-ob_y[ii],2 ) ) < ob_r + 2*GRID_SIZE ) {
					continue outer;
				}
			}
			for ( int ii = 0; ii < cp_n; ii++ ) {
				if ( Math.sqrt( Math.pow( ob_x[i]-cp_x[ii],2 ) + Math.pow( ob_y[i]-cp_y[ii],2 ) ) < CP_R + ob_r + GRID_SIZE ) {
					continue outer;
				}
			}			
			i++;
		}
		
		// draw background and mask image
		Graphics2D g_bg = (Graphics2D)bg.getGraphics();
		Graphics2D g_mask = (Graphics2D)mask.getGraphics();
		
		// background
		g_bg.setColor( Color.WHITE );
		g_bg.fillRect( 0,0,WIDTH,HEIGHT );
		g_mask.setColor( new Color( 55,0,0 ) ); //all track
		g_mask.fillRect( 0,0,WIDTH,HEIGHT );

		// draw obstacles
		for( int i = 0; i < ob_n; i++ ) {
			g_bg.setColor( new Color( 60,120,240 ) );
			g_bg.drawOval( ob_x[i]-ob_r,ob_y[i]-ob_r,2*ob_r,2*ob_r );
			g_mask.setColor( new Color( 66,0,0 ) ); // wall
			g_mask.fillOval( ob_x[i]-ob_r,ob_y[i]-ob_r,2*ob_r,2*ob_r );
		}		
				
		// draw checkpoints
		for( int i = 0; i < cp_n; i++ ) {
			g_bg.setColor( new Color( 255,255,100 ) );
			g_bg.fillOval( cp_x[i]-CP_R,cp_y[i]-CP_R,2*CP_R,2*CP_R );
			g_bg.setColor( new Color( 60,120,240,200 ) );
			if ( i == cp_n-1 ) {
				g_bg.drawString( "S/F",cp_x[i]-8,cp_y[i]+3 );
			}
			else {
				g_bg.drawString( String.valueOf( i+1 ),cp_x[i]-3,cp_y[i]+3 );
			}
			g_mask.setColor( new Color( i,0,0 ) );
			g_mask.fillOval( cp_x[i]-CP_R,cp_y[i]-CP_R,2*CP_R,2*CP_R );
		}
		
		// grid lines
		g_bg.setColor( GRID_COLOR );
		for ( int x = 0; x <= WIDTH; x += GRID_SIZE ) {
			g_bg.drawLine( x, 0, x, HEIGHT );
		}
		for ( int y = 0; y <= HEIGHT; y += GRID_SIZE ) {
			g_bg.drawLine( 0, y, WIDTH, y );
		}
		
	}
		
	private void newRace( int numPlayers )
	{
		// init players
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( i < numPlayers ) {
				p_x[i] = Math.round(cp_x[cp_n-1]/GRID_SIZE)*GRID_SIZE;
				p_y[i] = Math.round(cp_y[cp_n-1]/GRID_SIZE)*GRID_SIZE;
			}
			else {
				p_x[i] = 0;
				p_y[i] = 0;
			}
			p_u[i] = 0;
			p_v[i] = 0;
			p_target[i] = 0;
		}
		
		// misc inits
		currentPlayer = 0;
		t = 0;
		scoreboard = "";
	}
	
	private void initSimulateStep() {
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
				p_i[i] = 0;
				p_x0[i] = p_x[i];
				p_y0[i] = p_y[i];
		}
		t0 = t;
		tm0 = Calendar.getInstance().getTimeInMillis( );
	}
	
	private void simulateStep()
	{
		t = t0 + Calendar.getInstance().getTimeInMillis()-tm0;
		
		if ( t > t0 + 1000 ) {
			t = t0 + 1000;
		}
		
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( p_x[i] != 0 ) {
		        int pixs = Math.max( Math.abs( p_u[i] ), Math.abs( p_v[i] ) );
				double dx = ( (double)p_u[i] ) / ( (double)pixs );
				double dy = ( (double)p_v[i] ) / ( (double)pixs );
				
				while( t0 + (p_i[i])*(1000.0/pixs) < t ) {
					//System.out.println( "p " + i + " x " + p_x[i] );
					//System.out.println( "p " + i + " y " + p_y[i] );
					//System.out.println( "p " + i + " i " + p_i[i] );
					//System.out.println( "p " + i + " f " + (t0 + (p_i[i])*(1.0/pixs)) );
					//System.out.println( "p " + i + " t " + time );
					p_i[i]++;
					p_x[i] = p_x0[i] + (int)Math.round( (p_i[i])*dx );
					p_y[i] = p_y0[i] + (int)Math.round( (p_i[i])*dy );
					
					
					//check
					Color color = new Color( mask.getRGB( p_x[i],p_y[i] ) );
					//System.out.println( "color: " + color );
					if ( color.getRed() == 55 ) { // on track
						//ok, still racing
					}
					else if ( color.getRed() < 55 ) { // hit checkpoint
						if ( p_target[i] == color.getRed() ) { // hit target checkpoint
							p_target[i]++;
							if ( p_target[i] == cp_n ) { //finished
								scoreboard += "player " + String.valueOf( i+1 ) + ": " + String.valueOf( t/1000.0 ) + "\n"; 
								System.out.println( "p " + i + " finished" );
								p_x[i] = 0; // out of game
								break;
							}
						}
					}
					else { // hit wall (66)
						p_x[i] = 0; // out of game
						System.out.println( "p " + i + " hit wall" );
						break;
					}
					
				}
			}
		}
		
		int n = 0;
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( p_x[i] != 0 ) { 
				n++;
			}
		}

		if ( n == 0 ) {
			state = MENU_STATE;
		}
		else if ( t == t0+1000 ) {
			while ( p_x[currentPlayer] == 0 ) {
				currentPlayer++;
			}
			state = INPUT_STATE;
		}
	}

	private int random( int min, int max ) {
		return min + (int)Math.round( (max-min)*Math.random() );
	}

}