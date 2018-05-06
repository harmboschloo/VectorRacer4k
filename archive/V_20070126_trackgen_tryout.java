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
import java.awt.geom.Line2D;

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
					newCircuit2();
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
	 *  new circuit.
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
		

	/**
	 *  new circuit.
	 */
	private void newCircuit2()
	{
		System.out.println( "newCircuit2" );
		// 
		int n = 10;
		int[] x = new int[n];
		int[] y = new int[n];
		int count = 1001;

		while( count > 1000 ) {
			count = 0;
			x[0] = random( 30, WIDTH-30 );
			y[0] = random( 30, HEIGHT-30 );
			outer:for ( int i = 1; i < n;  ) {
				count++;
				if ( count > 1000 ) {
					System.out.println( "To many tries!" );
					break;
				}
				
				do {
					x[i] = random( x[i-1]-300, x[i-1]-80,x[i-1]+80, x[i-1]+300 );
					y[i] = random( y[i-1]-300, y[i-1]-80,y[i-1]+80, y[i-1]+300 );
				}
				while( x[i] < 30 || x[i] > WIDTH-30 || y[i] < 30 || y[i] > HEIGHT-30  );
				
				// check angle [simplify]
				if ( i > 1 ) {
					double angle = Math.acos( 
						( (x[i-2]-x[i-1])*(x[i]-x[i-1])+(y[i-2]-y[i-1])*(y[i]-y[i-1]) )/
						( Math.sqrt( Math.pow( (x[i-2]-x[i-1]),2 ) + Math.pow( (y[i-2]-y[i-1]), 2 ) )*
						Math.sqrt( Math.pow( (x[i]-x[i-1]),2 ) + Math.pow( (y[i]-y[i-1]), 2 ) ) ) 
					);
					//System.out.println( "angle: " + 180*angle/Math.PI );
					if ( angle < 30*Math.PI/180.0 ) {
						continue outer;
					}
				}
				
				
				//check intersections
				// the new line
				Line2D.Float line = new Line2D.Float( x[i-1],y[i-1],x[i],y[i] );
				for ( int ii = 1; ii < i-1; ii++ ) {
					if( line.intersectsLine( x[ii-1],y[ii-1],x[ii],y[ii] ) ) {
						continue outer;
					}
				}
				
				for ( int ii = 0; ii < i-1; ii++ ) {
					// check distances from every previous point to the new point
					double distance = Math.sqrt( 
						Math.pow( (x[ii]-x[i]),2 ) + Math.pow( (y[ii]-y[i]),2 )
					);
					
					if ( distance < 55 ) {
						continue outer;
					}
					else {
						//System.out.println( "distance point " + (i+1) + " point " + (ii+1) + ": " + distance );
					}
	
					// check distance from every previous point to the new line
					// a line from a preivous point, perpendicular to the new line
					Line2D.Float line2 = new Line2D.Float( x[ii]+1000*(y[i]-y[i-1]),y[ii]+1000*-(x[i]-x[i-1]),x[ii]-1000*(y[i]-y[i-1]),y[ii]-1000*-(x[i]-x[i-1]) );
					if( line2.intersectsLine( line ) ) {
						// check distance to line
						distance = Math.abs( 
							(x[i]-x[i-1])*(y[i-1]-y[ii])-(x[i-1]-x[ii])*(y[i]-y[i-1])
						)/Math.sqrt( 
							Math.pow( (x[i]-x[i-1]),2 ) + Math.pow( (y[i]-y[i-1]),2 )
						);
						if ( distance < 55 ) {
							continue outer;
						}
						else {
							//System.out.println( "distance line " + i + " point " + (ii+1) + ": " + distance );
							
						}
					}
				}
				
				for ( int ii = 1; ii < i-1; ii++ ) {
					// check distance from new point to every previous line
					// a line from the new point, perpendicular to a previous line
					Line2D.Float line3 = new Line2D.Float( x[i]+1000*(y[ii]-y[ii-1]),y[i]+1000*-(x[ii]-x[ii-1]),x[i]-1000*(y[ii]-y[ii-1]),y[i]-1000*-(x[ii]-x[ii-1]) );
					if( line3.intersectsLine( x[ii-1],y[ii-1],x[ii],y[ii] ) ) {
						// check distance to line
						double distance = Math.abs( 
							(x[ii]-x[ii-1])*(y[ii-1]-y[i])-(x[ii-1]-x[i])*(y[ii]-y[ii-1])
						)/Math.sqrt( 
							Math.pow( (x[ii]-x[ii-1]),2 ) + Math.pow( (y[ii]-y[ii-1]),2 )
						);
						if ( distance < 55 ) {
							continue outer;
						}
						else {
							//System.out.println( "distance point " + (i+1) + " line " + ii + ": " + distance );
						}	
					}
				}
				
				System.out.println( "next point: " + (i+2) );
				
				i++;
			}
		}
		
		// debug
		cp_x[0] = x[0];
		cp_y[0] = y[0];
		
		// draw background and mask image
		Graphics2D g_bg = (Graphics2D)bg.getGraphics();
		//Graphics2D g_mask = (Graphics2D)mask.getGraphics();
		
		// background
		g_bg.setColor( Color.WHITE );
		g_bg.fillRect( 0,0,WIDTH,HEIGHT );
		//g_mask.setColor( new Color( 66,0,0 ) ); //all wall

		g_bg.setColor( Color.BLUE );
		g_bg.setStroke( new BasicStroke( 50, BasicStroke.CAP_ROUND , BasicStroke.JOIN_ROUND ) );
		for ( int i = 0; i < n; i++ ) {
			g_bg.drawPolyline( x,y,n );
		}
		
		// draw circuit
		/*GeneralPath path = new GeneralPath();
		path.moveTo( cp_x[0],cp_y[0] );
		for( int i = 1; i < cp_n; i++ ) {
			path.lineTo( cp_x[i],cp_y[i] );
			//path.quadTo(100.0f,100.0f,225.0f,125.0f);
			//path.curveTo(260.0f,100.0f,130.0f,50.0f,225.0f,0.0f);
		}
		path.closePath();

		g_bg.setColor( Color.BLUE );
		g_bg.setStroke( new BasicStroke( 30, BasicStroke.CAP_ROUND , BasicStroke.JOIN_ROUND ) );
		g_bg.draw(path);
		*/
		
		/*for( int i = 0; i < cp_n; i++ ) {
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
		}*/
		
		// grid lines
		g_bg.setColor( GRID_COLOR );
		g_bg.setStroke( new BasicStroke() );
		for ( int g_x = 0; g_x <= WIDTH; g_x += GRID_SIZE ) {
			g_bg.drawLine( g_x, 0, g_x, HEIGHT );
		}
		for ( int g_y = 0; g_y <= HEIGHT; g_y += GRID_SIZE ) {
			g_bg.drawLine( 0, g_y, WIDTH, g_y );
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

	private int random( int min1, int max1, int min2, int max2 ) {
		if ( Math.random() < 0.5 ) {
			return min1 + (int)Math.round( (max1-min1)*Math.random() );
		}
		else {
			return min2 + (int)Math.round( (max2-min2)*Math.random() );
		}
	}
}