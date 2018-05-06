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
import java.awt.BasicStroke;
import java.awt.Point;
import java.util.Vector;
//import java.awt.geom.GeneralPath;
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
	private static final Color SF_COLOR = Color.YELLOW;
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
	private static final int AI_INPUT_STATE = 3;
	private static final int SIM_STATE = 4;
	
	private static final int MENU_RACE = 225;
	private static final int MENU_PLAYERS = 245;
	private static final int MENU_AIS = 265;
	private static final int MENU_NEW_TRACK = 285;

	private int _state = MENU_STATE;
	private BufferedImage _track = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
	private BufferedImage _overlay;
	private int[] _p_x = new int[MAX_PLAYERS]; // player x position 
	private int[] _p_y = new int[MAX_PLAYERS]; // player y position 
	private int[] _p_x0 = new int[MAX_PLAYERS]; // player x position at start of simulation
	private int[] _p_y0 = new int[MAX_PLAYERS]; // player y position at start of simulation
	private int[] _p_u = new int[MAX_PLAYERS]; // player speed in x direction 
	private int[] _p_v = new int[MAX_PLAYERS]; // player speed in y direction 
	private boolean[] _p_halfway = new boolean[MAX_PLAYERS]; // player halfway indicatro
	private int[] _p_i = new int[MAX_PLAYERS]; // player pixel count during simulation
	private int _h_n = 1; // number of human players
	private int _ai_n = 1; // number of ai players
	private int _currentPlayer = 0; // current player
	private int _sf_x = 315; // start/finish x position
	private int _sf_y = 120; // start/finish y position
	private int _du = 0; // change of current player speed in x direction 
	private int _dv = 0; // change of current player speed in y direction 
	private long _t = 0; // time
	private long _t0 = 0; // time at start of simulation
	private long _tm0 = 0; // time in millies at start of simulation
	private int _menu = MENU_RACE; // current menu indicator position
	private int _score_pos; // current score position
	private int[][] _trace = new int[WIDTH][HEIGHT];
	private int _tv;
	private int _ai_level = 3;
	private int _halfway;
	
	/**
	 * Entry point.
	 * @param argv The arguments passed in to the program.
	 */
	public static void main(String argv[]) {
		new V();
	}

	
	private V()	{
		super("VR4K");
		
		setSize( WIDTH+20,HEIGHT+50 );
		//setResizable( false );
		show();
		
		// create track
		newTrack();
		newOverlay();
		
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
			
			if ( _state == SIM_STATE ) {
				simulateStep();
			}
			
			
			if ( _state == AI_INPUT_STATE ) {
				runAI();				
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
	protected void processKeyEvent( KeyEvent e ) {
		if ( e.getID() == KeyEvent.KEY_PRESSED ) {
		
			if ( _state == INPUT_STATE  ) {
				if ( e.getKeyCode() == KeyEvent.VK_UP ) {
					_dv -= GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_DOWN ) {
					_dv += GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					_du += GRID_SIZE;
				}
				if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					_du -= GRID_SIZE;
				}
				if ( _du > GRID_SIZE ) { _du = GRID_SIZE; }
				if ( _du < -GRID_SIZE ) { _du = -GRID_SIZE; }
				if ( _dv > GRID_SIZE ) { _dv = GRID_SIZE; }
				if ( _dv < -GRID_SIZE ) { _dv = -GRID_SIZE; }
				
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					processStep();					
				}
			}	
			
			if ( _state == MENU_STATE ) {
				if ( e.getKeyCode() == KeyEvent.VK_UP ) {
					if ( _menu == MENU_RACE ) {
						_menu = MENU_NEW_TRACK;
					}
					else {
						_menu -= 20;
					}
				}
				if ( e.getKeyCode() == KeyEvent.VK_DOWN ) {
					if ( _menu == MENU_NEW_TRACK ) {
						_menu = MENU_RACE;
					}
					else {
						_menu += 20;
					}
				}	
				if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					if ( _menu == MENU_PLAYERS && pn() < MAX_PLAYERS ) {
						_h_n++;
					}
					if ( _menu == MENU_AIS && pn() < MAX_PLAYERS ) {
						_ai_n++;
					}
				}					
				if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					if ( _menu == MENU_PLAYERS && _h_n > 0 ) {
						_h_n--;
					}
					if ( _menu == MENU_AIS && _ai_n > 0 ) {
						_ai_n--;
					}
				}	
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					if ( _menu == MENU_RACE ) {
						_state = PROCESS_STATE;
						createTrace();
						newRace();
						doStep();
					}
					if ( _menu == MENU_NEW_TRACK ) {
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
				_state = MENU_STATE;
			}
		}
	}
	
	private void doStep() {
		 if ( _currentPlayer < _h_n ) {
			 _state = INPUT_STATE;			 
		 }
		 else {
			 _state = AI_INPUT_STATE;
		 }
	}
	
	private void processStep() {
		
		_state = PROCESS_STATE;
		_p_u[_currentPlayer] += _du;
		_p_v[_currentPlayer] += _dv;
		
		_du = 0;
		_dv = 0;
		
		// next player
		while( true ) {
			_currentPlayer++;
			if ( pn() == _currentPlayer ) {
				_currentPlayer = 0;
				initSimulateStep();
				_state = SIM_STATE;
				break;
			}
			else if ( _p_x[_currentPlayer] != 0 ) {
				doStep();
				break;
			}
		}
	}
	
	/** 
	*  @param g The graphics context in which to paint
	*/
	private void draw( Graphics2D g ) {
		g.translate(10,35);	
		
		// paper & track
		g.drawImage( _track, 0, 0, null );
		g.drawImage( _overlay, 0, 0, null );
		
		// players
		if ( _state != MENU_STATE ) {
			for ( int i = 0; i < MAX_PLAYERS; i++ ) {
				if ( _p_x[i] != 0 ) {
					g.setColor( PLAYER_COLORS[i] );
					g.drawOval( _p_x[i]-PLAYER_R,_p_y[i]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
				}
			}
		}
		
		// input selection
		if ( _state == INPUT_STATE ) {
			g.setColor( PLAYER_COLORS[_currentPlayer] );
			g.drawString( "player " + String.valueOf( _currentPlayer+1 ),50,13 );
			g.drawLine( 
				_p_x[_currentPlayer],
				_p_y[_currentPlayer],
				_p_x[_currentPlayer]+_p_u[_currentPlayer]+_du,
				_p_y[_currentPlayer]+_p_v[_currentPlayer]+_dv );
			//[change]
			g.fillOval( _p_x[_currentPlayer]+_p_u[_currentPlayer]-PLAYER_R/2,_p_y[_currentPlayer]+_p_v[_currentPlayer]-PLAYER_R/2,PLAYER_R,PLAYER_R );
		}
		
		// main menu
		if ( _state == MENU_STATE ) {
			g.setColor( new Color( 0,0,0,50 ) );
			g.fillRect( 270,190,100,100 );
			g.setColor( Color.BLACK );
			g.drawRect( 270,190,100,100 );
			g.drawOval( 285,_menu-8,8,8 );
			g.drawString( "race",298,MENU_RACE );
			g.drawString( "players: " + String.valueOf( _h_n ),298,MENU_PLAYERS );
			g.drawString( "ais: " + String.valueOf( _ai_n ),298,MENU_AIS );
			g.drawString( "new track",298,MENU_NEW_TRACK );
		}

		// time
		g.setColor( Color.BLACK );
		g.drawString( String.valueOf( _t/1000.0 ),5,13 );

	}
	
	/**
	 *  new circuit.
	 */
	private void newTrack()	{
		// create track
		int n = 7;
		int[] x = new int[n];
		int[] y = new int[n];
		x[0] = _sf_x-80;
		y[0] = _sf_y;
		x[1] = _sf_x+80;
		y[1] = _sf_y;
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
		Graphics2D g = (Graphics2D)_track.getGraphics();
		
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
		int[] sfx = { _sf_x,_sf_x-2*GRID_SIZE,_sf_x-2*GRID_SIZE };
		int[] sfy = { _sf_y,_sf_y+TRACK_SIZE/2+1,_sf_y-TRACK_SIZE/2-1 };
		g.setColor( SF_COLOR );
		g.setStroke( new BasicStroke() );
		g.fillPolygon( sfx,sfy,3 );
	}
		
	private void newOverlay() {
		// draw new overlay
		_overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = (Graphics2D)_overlay.getGraphics();

		g.setColor( GRID_COLOR );
		for ( int g_x = 0; g_x < WIDTH; g_x += GRID_SIZE ) {
			g.drawLine( g_x, 0, g_x, HEIGHT-1 );
		}
		for ( int g_y = 0; g_y < HEIGHT; g_y += GRID_SIZE ) {
			g.drawLine( 0, g_y, WIDTH-1, g_y );
		}
	}
	
	private void newRace() {
		// init players
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( i < pn() ) {
				_p_x[i] = _sf_x;
				_p_y[i] = _sf_y;
			}
			else {
				_p_x[i] = 0;
				_p_y[i] = 0;
			}
			_p_u[i] = 0;
			_p_v[i] = 0;
			_p_halfway[i] = false;
		}
		
		// misc inits
		_currentPlayer = 0;
		_t = 0;
		_score_pos = 28;
		
		// new overlay
		newOverlay();
	}
	
	private void initSimulateStep() {
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
				_p_i[i] = 0;
				_p_x0[i] = _p_x[i];
				_p_y0[i] = _p_y[i];
		}
		_t0 = _t;
		_tm0 = Calendar.getInstance().getTimeInMillis( );
	}
	
	private void simulateStep()	{
		_t = _t0 + 2*(Calendar.getInstance().getTimeInMillis()-_tm0); // speed-up 2x
		
		if ( _t > _t0 + 1000 ) {
			_t = _t0 + 1000;
		}
		
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( _p_x[i] != 0 ) {
		        int pixs = Math.max( Math.abs( _p_u[i] ), Math.abs( _p_v[i] ) );
				double dx = ( (double)_p_u[i] ) / ( (double)pixs );
				double dy = ( (double)_p_v[i] ) / ( (double)pixs );
				
				while( _t0 + (_p_i[i])*(1000.0/pixs) < _t ) {
					//System.out.println( "p " + i + " x " + p_x[i] );
					//System.out.println( "p " + i + " y " + p_y[i] );
					//System.out.println( "p " + i + " i " + p_i[i] );
					//System.out.println( "p " + i + " f " + (t0 + (p_i[i])*(1.0/pixs)) );
					//System.out.println( "p " + i + " t " + time );
					_p_i[i]++;
					_p_x[i] = _p_x0[i] + (int)Math.round( (_p_i[i])*dx );
					_p_y[i] = _p_y0[i] + (int)Math.round( (_p_i[i])*dy );
					
					//System.out.println( "trace " + _trace[_p_x[i]][_p_y[i]] );
					
					// check halfway [todo, check 1/3, 2/3]
					if ( Math.abs( _trace[_p_x[i]][_p_y[i]]-_halfway ) < 10 ) {
						_p_halfway[i] = true;
					}
										
					// check surface
					Color color = new Color( _track.getRGB( _p_x[i],_p_y[i] ) );
					//System.out.println( "color: " + color );
					if ( color.equals( WALL_COLOR ) ) {
						_p_x[i] = 0; // out of game
						System.out.println( "p " + i + " hit wall" );
						break;
					}
					if ( color.equals( SF_COLOR ) ) { // start/finish
						if ( _p_halfway[i] ) { // finished
							Graphics2D g = (Graphics2D)_overlay.getGraphics();
							g.setColor( PLAYER_COLORS[i] );
							//[todo: check time calc & order of finished players could be wrong!]
							g.drawString( String.valueOf( (_t0 + Math.round( 1000.0*_p_i[i]/pixs ))/1000.0 ) + " player " + String.valueOf( i+1 ), 5, _score_pos );
							_score_pos += 15;
							System.out.println( "p " + i + " finished" );
							_p_x[i] = 0; // out of game
							break;
						}
					}
				}
			}
		}
		
		int n = 0;
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( _p_x[i] != 0 ) { 
				n++;
			}
		}

		if ( n == 0 ) {
			_state = MENU_STATE;
		}
		else if ( _t == _t0+1000 ) {
			while ( _p_x[_currentPlayer] == 0 ) {
				_currentPlayer++;
			}
			doStep();
		}
	}

	private void createTrace() {
		int x,y;
		
		// init 
		for ( x = 0; x < WIDTH; x++ ) {
			for ( y = 0; y < HEIGHT; y++ ) {
				Color color = new Color( _track.getRGB( x,y ) );
				_trace[x][y] = -2;
				if ( color.equals( SF_COLOR ) ) { // start/finish
					_trace[x][y] = -1;
				}
				if ( color.equals( TRACK_COLOR ) ) {
					_trace[x][y] = 0;
				}
			}
		}
		
		// trace
        Vector<Point> currentPoints = new Vector<Point>();
        Vector<Point> changedPoints = new Vector<Point>();
        int i,d,lx,ly,lv;
        int[] dx = { -1,1,0,0 };
        int[] dy = { 0,0,-1,1 };  
        
        currentPoints.add( new Point( _sf_x-2*GRID_SIZE,_sf_y ) );
        _trace[_sf_x-2*GRID_SIZE][_sf_y] = 1;
        while ( !currentPoints.isEmpty () ) {
            for ( i = 0; i < currentPoints.size(); i++ ) {
            	x = currentPoints.get( i ).x;
            	y = currentPoints.get( i ).y;
                // check around
            	for ( d = 0; d < dx.length; d++ ) {
	            	lx = x+dx[d];
	            	ly = y+dy[d];
            		lv = _trace[lx][ly];
                    if ( lv == 0 || lv > _trace[x][y]+1 ) {
                    	_trace[lx][ly] = _trace[x][y]+1;
                    	changedPoints.add( new Point( lx,ly ) );
                    }
                } // end for loop
            } // end for loop
            // reset points
            currentPoints.clear();
            currentPoints.addAll( changedPoints );
            changedPoints.clear();
        }  // end while loop

        _halfway = (_trace[_sf_x+GRID_SIZE][_sf_y])/2;
	}

	private void runAI() {
		_state = PROCESS_STATE;
		_tv = Integer.MAX_VALUE;
		runAI( 
			_p_x[_currentPlayer], 
			_p_y[_currentPlayer],
			_p_u[_currentPlayer], 
			_p_v[_currentPlayer],
			0,
			0,
			_ai_level );
		
		//System.out.println( "du " + _du );
		//System.out.println( "dv " + _dv );
		//System.out.println( "tv " + _tv );
		
		processStep();
	}
	
	private void runAI( int x0, int y0, int u0, int v0, int du0, int dv0, int n ) {
		n--;
		int tv = 0;
		int du = -GRID_SIZE;
		//if ( _trace[x0][y0] < 0 ) { du = GRID_SIZE; } // go forward on start/finish
		
		for ( ; du < 2*GRID_SIZE; du += GRID_SIZE ) {
			for ( int dv = -GRID_SIZE; dv < 2*GRID_SIZE; dv += GRID_SIZE ) {
				if ( n == (_ai_level-1) ) {
					du0 = du;
					dv0 = dv;
				}
				
				int u = u0+du;
				int v = v0+dv;
		        int pixs = Math.max( Math.abs( u ), Math.abs( v ) );
				double dx = ( (double)u ) / ( (double)pixs );
				double dy = ( (double)v ) / ( (double)pixs );
				boolean hitWall = false;
				
				int x = x0;
				int y = y0;
				for ( int i = 1; i <= pixs; i++ ) {
					x = x0 + (int)Math.round( i*dx );
					y = y0 + (int)Math.round( i*dy );

					// check surface
					tv = _trace[x][y];
					if ( tv == -2 ) {
						hitWall = true;
						break;
					}
					if ( tv == -1 ) { // start/finish
						if ( _p_halfway[_currentPlayer] ) {
							int f = -n*100+i;
							if ( f < _tv ) {
								_tv = f;
								_du = du0;
								_dv = dv0;
							}
						}
						else {
							hitWall = true;
						}
						break;
					}
				}
				
				if ( !hitWall ) {
					// next level
					if ( n > 0 ) {
						runAI( x,y,u,v,du0,dv0,n );
					}
					else { // last level
						if ( tv > 0 ) {
							if ( tv > _halfway/2 ) { tv += random( -GRID_SIZE,GRID_SIZE ); }
							if ( tv < _tv ) {
								_tv = tv;
								_du = du0;
								_dv = dv0;
							}
						}								
					}
				}
			}
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
	
	private int pn() {
		return (_h_n+_ai_n);
	}
}