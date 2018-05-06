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
//import java.awt.geom.GeneralPath;
import java.awt.BasicStroke;
//import java.awt.Polygon;
//import java.awt.geom.Line2D;

/**
 * @author Harm Boschloo
 */
// [check to remove public, gives error on run after optimization]
class V extends JFrame
{

	private static final int WIDTH = 631;
	private static final int HEIGHT = 481;
	private static final int TRACK_SIZE = 70;
	private static final int GRID_SIZE = 15;
	
	private static final Color TRACK_COLOR = Color.WHITE;
	private static final Color WALL_COLOR = new Color( 60,120,240 );
	private static final Color SF_COLOR = new Color( 255,255,100 );
	private static final Color GRID_COLOR = new Color( 195,210,240,120 );

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

	private static final int PROCESS_STATE = 0;
	private static final int MENU_STATE = 1;
	private static final int INPUT_STATE = 2;
	private static final int SIM_STATE = 3;
	
	private static final int MENU_RACE = 225;
	private static final int MENU_PLAYERS = 245;
	private static final int MENU_NEW_TRACK = 265;

	private int state = MENU_STATE;
	private BufferedImage track = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
	private BufferedImage overlay;
	private int[] p_x = new int[MAX_PLAYERS]; // player x position 
	private int[] p_y = new int[MAX_PLAYERS]; // player y position 
	private int[] p_x0 = new int[MAX_PLAYERS]; // player x position at start of simulation
	private int[] p_y0 = new int[MAX_PLAYERS]; // player y position at start of simulation
	private int[] p_u = new int[MAX_PLAYERS]; // player speed in x direction 
	private int[] p_v = new int[MAX_PLAYERS]; // player speed in y direction 
	private int[] p_target = new int[MAX_PLAYERS]; // player target zone
	private int[] p_i = new int[MAX_PLAYERS]; // player pixel count during simulation (?)
	private int p_n = 2; // number of players
	private int currentPlayer = 0; // current player
	private int sf_x = 315; // start/finish x position
	private int sf_y = 120; // start/finish y position
	private int du = 0; // change of current player speed in x direction 
	private int dv = 0; // change of current player speed in y direction 
	private long t = 0; // time
	private long t0 = 0; // time at start of simulation
	private long tm0 = 0; // time in millies at start of simulation
	private int menu = MENU_RACE; // current menu indicator position
	private int score_pos; // current score position
	
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
		
		// create track
		newTrack();
		
		// draw buffer
		createBufferStrategy(2);
		BufferStrategy strategy = getBufferStrategy();
	
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
				if ( e.getKeyCode() == KeyEvent.VK_UP ) {
					dv -= GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_DOWN ) {
					dv += GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					du += GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					du -= GRID_SIZE;
				}
				if ( du > GRID_SIZE ) { du = GRID_SIZE; }
				if ( du < -GRID_SIZE ) { du = -GRID_SIZE; }
				if ( dv > GRID_SIZE ) { dv = GRID_SIZE; }
				if ( dv < -GRID_SIZE ) { dv = -GRID_SIZE; }
				
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
							currentPlayer = 0;
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
				if ( e.getKeyCode() == KeyEvent.VK_UP ) {
					if ( menu == MENU_RACE ) {
						menu = MENU_NEW_TRACK;
					}
					else {
						menu -= 20;
					}
				}
				if ( e.getKeyCode() == KeyEvent.VK_DOWN ) {
					if ( menu == MENU_NEW_TRACK ) {
						menu = MENU_RACE;
					}
					else {
						menu += 20;
					}
				}	
				if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					if ( menu == MENU_PLAYERS && p_n < MAX_PLAYERS ) {
						p_n++;
					}
				}					
				if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					if ( menu == MENU_PLAYERS && p_n > 1 ) {
						p_n--;
					}
				}	
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					if ( menu == MENU_RACE ) {
						state = PROCESS_STATE;
						newRace();
						state = INPUT_STATE;
					}
					if ( menu == MENU_NEW_TRACK ) {
						newTrack();
					}
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
		
		// paper & track
		g.drawImage( track, 0, 0, null );
		g.drawImage( overlay, 0, 0, null );
		
		// players
		if ( state != MENU_STATE ) {
			for ( int i = 0; i < MAX_PLAYERS; i++ ) {
				if ( p_x[i] != 0 ) {
					g.setColor( PLAYER_COLORS[i] );
					g.drawOval( p_x[i]-PLAYER_R,p_y[i]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
				}
			}
		}
		
		// input selection
		if ( state == INPUT_STATE ) {
			g.setColor( PLAYER_COLORS[currentPlayer] );
			g.drawString( "player " + String.valueOf( currentPlayer+1 ),50,13 );
			g.drawLine( 
				p_x[currentPlayer],
				p_y[currentPlayer],
				p_x[currentPlayer]+p_u[currentPlayer]+du,
				p_y[currentPlayer]+p_v[currentPlayer]+dv );
			//[change]
			g.fillOval( p_x[currentPlayer]+p_u[currentPlayer]-PLAYER_R/2,p_y[currentPlayer]+p_v[currentPlayer]-PLAYER_R/2,PLAYER_R,PLAYER_R );
		}
		
		// main menu
		if ( state == MENU_STATE ) {
			g.setColor( new Color( 0,0,0,50 ) );
			g.fillRect( 270,190,100,100 );
			g.setColor( Color.BLACK );
			g.drawRect( 270,190,100,100 );
			g.drawOval( 285,menu-8,8,8 );
			g.drawString( "race",298,MENU_RACE );
			g.drawString( "players: " + String.valueOf( p_n ),298,MENU_PLAYERS );
			g.drawString( "new track",298,MENU_NEW_TRACK );
		}

		// time
		g.setColor( Color.BLACK );
		g.drawString( String.valueOf( t/1000.0 ),5,13 );

	}
	
	/**
	 *  new circuit.
	 */
	private void newTrack()
	{
		
		// create track
		int n = 7;
		int[] x = new int[n];
		int[] y = new int[n];
		x[0] = sf_x-80;
		y[0] = sf_y;
		x[1] = sf_x+80;
		y[1] = sf_y;
		x[2] = random( 440+TRACK_SIZE/2+10,WIDTH-TRACK_SIZE/2-10 );
		y[2] = random( 0+TRACK_SIZE/2+10,240-TRACK_SIZE/2-10 );
		x[3] = random( 440+TRACK_SIZE/2+10,WIDTH-TRACK_SIZE/2-10 );
		y[3] = random( 240+TRACK_SIZE/2+10,HEIGHT-TRACK_SIZE/2-10 );
		x[4] = random( 200+TRACK_SIZE/2+10,440-TRACK_SIZE/2-10 );
		y[4] = random( 240+TRACK_SIZE/2+10,HEIGHT-TRACK_SIZE/2-10 );
		x[5] = random( 0+TRACK_SIZE/2+10,200-TRACK_SIZE/2-10 );
		y[5] = random( 240+TRACK_SIZE/2+10,HEIGHT-TRACK_SIZE/2-10 );
		x[6] = random( 0+TRACK_SIZE/2+10,200-TRACK_SIZE/2-10 );
		y[6] = random( 0+TRACK_SIZE/2+10,240-TRACK_SIZE/2-10 );
		
		
		// draw new track 
		Graphics2D g = (Graphics2D)track.getGraphics();
		
		// background
		g.setColor( TRACK_COLOR );
		g.fillRect( 0,0,WIDTH,HEIGHT );
		
		// draw wall outline
		g.setColor( WALL_COLOR );
		g.setStroke( new BasicStroke( TRACK_SIZE+4,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
		g.drawPolygon( x,y,n );
		
		// draw track
		g.setColor( TRACK_COLOR );
		g.setStroke( new BasicStroke( TRACK_SIZE,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
		g.drawPolygon( x,y,n );
		
		// draw start/finish
		g.setColor( SF_COLOR );
		g.fillRect(sf_x-TRACK_SIZE/2,sf_y-TRACK_SIZE/2, TRACK_SIZE, TRACK_SIZE );
		
		// draw new overlay
		overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		g = (Graphics2D)overlay.getGraphics();

		g.setColor( GRID_COLOR );
		for ( int g_x = 0; g_x < WIDTH; g_x += GRID_SIZE ) {
			g.drawLine( g_x, 0, g_x, HEIGHT-1 );
		}
		for ( int g_y = 0; g_y < HEIGHT; g_y += GRID_SIZE ) {
			g.drawLine( 0, g_y, WIDTH-1, g_y );
		}	
		
	}
		
	private void newRace()
	{
		// init players
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( i < p_n ) {
				p_x[i] = sf_x;
				p_y[i] = sf_y;
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
		score_pos = 28;
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
					
					
					// check zone
					if ( p_target[i] == 0 ) {
						if ( p_x[i] > 440 ) { 
							p_target[i]++; 
						}					
					}
					if ( p_target[i] == 1 ) {
						if ( p_y[i] > 240 ) { 
							p_target[i]++; 
						}					
					}
					if ( p_target[i] == 2 ) {
						if ( p_x[i] < 200 ) { 
							p_target[i]++; 
						}					
					}					
					if ( p_target[i] == 3 ) {
						if ( p_y[i] < 240 ) { 
							p_target[i]++; 
						}					
					}
					
					// check surface
					Color color = new Color( track.getRGB( p_x[i],p_y[i] ) );
					//System.out.println( "color: " + color );
					if ( color.equals( WALL_COLOR ) ) {
						p_x[i] = 0; // out of game
						System.out.println( "p " + i + " hit wall" );
						break;
					}
					if ( color.equals( SF_COLOR ) ) { // start/finish
						if ( p_target[i] == 4 ) { // finished
							Graphics2D g = (Graphics2D)overlay.getGraphics();
							g.setColor( PLAYER_COLORS[i] );
							g.drawString( String.valueOf( t/1000.0 ) + " player " + String.valueOf( i+1 ), 5, score_pos );
							score_pos += 15;
							System.out.println( "p " + i + " finished" );
							p_x[i] = 0; // out of game
							break;
						}
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

	/*private int random( int min1, int max1, int min2, int max2 ) {
		if ( Math.random() < 0.5 ) {
			return min1 + (int)Math.round( (max1-min1)*Math.random() );
		}
		else {
			return min2 + (int)Math.round( (max2-min2)*Math.random() );
		}
	}*/
}