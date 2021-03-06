/**
* Vector Racer 4K 
* @author Harm Boschloo
* todo:
* improve sim loop and main loop
* check 4kminers game (for draw loop etc.)
* clean up code 
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
	
	private static final Color TRACK_COLOR = new Color( 255,255,255 );
	private static final Color WALL_COLOR = new Color( 60,120,240 );
	private static final Color SF_COLOR = new Color( 255,255,0 );
	private static final Color GRID_COLOR = new Color( 195,210,240,120 );

	private static final int MAX_PLAYERS = 10;
	private static final int PLAYER_R = 4;
    private static final Color[] PLAYER_COLORS = {
        new Color( 0,0,255 ),
        new Color( 0,192,0 ),
        new Color( 255,0,0 ),
        new Color( 255,128,0 ),
        new Color( 255,0,255 ),
        new Color( 64,128,128 ),
        new Color( 128,0,255 ),
        new Color( 0,192,192 ),
        new Color( 0,0,0 ),
        new Color( 128,128,128 ),
    };
    /*private static final Color[] PLAYER_COLORS = {
        new Color( 255,0,0 ),
        new Color( 255,160,0 ),
        new Color( 0,200,0 ),
        new Color( 0,0,200 ),
        new Color( 180,0,0 ),
        new Color( 120,0,200 ),
        new Color( 200,0,160 ),
        new Color( 0,160,200 ),
        new Color( 0,200,150 ),
        new Color( 0,0,0 )
    };*/

	private static final int PROCESS_STATE = 0;
	private static final int MENU_STATE = 1;
	private static final int INPUT_STATE = 2;
	private static final int AI_INPUT_STATE = 3;
	private static final int SIM_STATE = 4;
	
	private static final int MENU_RACE = 215;
	private static final int MENU_PLAYERS = 235;
	private static final int MENU_AIS = 255;
	private static final int MENU_NEW_TRACK = 275;

	private static final int WALL = -2;
	private static final int START_FINISH = -1;
	
	private static final  int SF_X = 315; // start/finish x position
	private static final  int SF_Y = 120; // start/finish y position
	private static final  int AI_LEVEL = 3; // level of the ai

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
	private int _du = 0; // change of current player speed in x direction 
	private int _dv = 0; // change of current player speed in y direction 
	private long _t = 0; // time
	private long _t0 = 0; // time at start of simulation
	private long _tm0 = 0; // time in millies at start of simulation
	private int _menu = MENU_RACE; // current menu indicator position
	private int _score_pos; // current score position
	private int[][] _trace = new int[WIDTH][HEIGHT];
	private int _tv;
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
	
		// main loop
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
				_state = PROCESS_STATE;
				_tv = Integer.MAX_VALUE;
				runAI( 
					_p_x[_currentPlayer], 
					_p_y[_currentPlayer],
					_p_u[_currentPlayer], 
					_p_v[_currentPlayer],
					0,
					0,
					AI_LEVEL );
				
				processStep();			
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
					if ( _menu == MENU_PLAYERS && (_h_n+_ai_n) < MAX_PLAYERS ) {
						_h_n++;
					}
					if ( _menu == MENU_AIS && (_h_n+_ai_n) < MAX_PLAYERS ) {
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
			if ( (_h_n+_ai_n) == _currentPlayer ) {
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
			
		// main menu
		if ( _state == MENU_STATE ) {
			g.setColor( new Color( 200,200,200,200 ) );
			g.fillRect( 270,190,100,100 );
			g.setColor( new Color( 0,0,0 ) );
			g.drawRect( 270,190,100,100 );
			g.drawOval( 285,_menu-8,8,8 );
			g.drawString( "race",298,MENU_RACE );
			g.drawString( "players: " + String.valueOf( _h_n ),298,MENU_PLAYERS );
			g.drawString( "ais: " + String.valueOf( _ai_n ),298,MENU_AIS );
			g.drawString( "new track",298,MENU_NEW_TRACK );
		}

		// time
		g.setColor( new Color( 0,0,0 ) );
		g.drawString( "time: " + String.valueOf( _t/1000.0 ),5,13 );
		
		// input selection
		if ( _state == INPUT_STATE ) {
			g.setColor( PLAYER_COLORS[_currentPlayer] );
			g.drawString( "player: " + String.valueOf( _currentPlayer+1 ),5,28 );
			g.drawLine( 
				_p_x[_currentPlayer],
				_p_y[_currentPlayer],
				_p_x[_currentPlayer]+_p_u[_currentPlayer]+_du,
				_p_y[_currentPlayer]+_p_v[_currentPlayer]+_dv );
			g.drawOval( _p_x[_currentPlayer]-PLAYER_R,_p_y[_currentPlayer]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
			float[] dash = { 3.0f };
			g.setStroke( new BasicStroke(
				1.0f,
                BasicStroke.CAP_BUTT, 
                BasicStroke.JOIN_BEVEL, 
                0.0f, 
                dash, 
                0.0f ) );
			g.drawLine( 
				_p_x[_currentPlayer]+_p_u[_currentPlayer],
				_p_y[_currentPlayer]+_p_v[_currentPlayer],
				_p_x[_currentPlayer],
				_p_y[_currentPlayer] );
		}

	}
	
	/**
	 *  new circuit.
	 */
	private void newTrack()	{
		// create track
		int offset = TRACK_SIZE/2+2;
		int ly1 = SF_Y+offset;
		int ly2 = SF_Y+3*offset;
		int lx2 = SF_X-2*GRID_SIZE-offset;
		int lx3 = SF_X+offset;
		int lx1 = lx2/2;
		int lx4 = lx3+(WIDTH-lx3)/2;
		//System.out.println( "lx1 " + lx1 );
		//System.out.println( "lx2 " + lx2 );
		//System.out.println( "lx3 " + lx3 );
		//System.out.println( "lx4 " + lx4 );
		//System.out.println( "ly1 " + ly1 );
		//System.out.println( "ly2 " + ly2 );
		int n = 12;
		int i = 0;
		int[] x = new int[n];
		int[] y = new int[n];
		// start/finish
		x[i] = lx2;
		y[i] = SF_Y;
		x[++i] = lx3;
		y[i] = SF_Y;
		// top right
		x[++i] = random( lx3+offset,lx4-offset );
		y[i] = random( 0+offset,ly2-offset );
		x[++i] = random( lx4,WIDTH-offset );
		y[i] = random( 0+offset,ly1-offset );
		// bottom right
		x[++i] = random( lx4+offset,WIDTH-offset );
		y[i] = random( ly2+offset,HEIGHT-offset );
		x[++i] = random( lx3+offset,lx4-offset );
		y[i] = random( ly2+offset,HEIGHT-offset );
		// bottom middle
		x[++i] = random( lx2+offset,lx3-offset );
		y[i] = random( ly2+offset,HEIGHT-offset );
		x[++i] = random( lx2+offset,lx3-offset );
		y[i] = random( ly1+offset,HEIGHT-offset );
		// bottom left
		x[++i] = random( 0+offset,lx2-offset );
		y[i] = random( ly2+offset,HEIGHT-offset );
		x[++i] = random( 0+offset,lx1-offset );
		y[i] = random( ly1+offset,ly2-offset );
		// top left
		x[++i] = random( 0+offset,lx1-offset );
		y[i] = random( 0+offset,ly1-offset );
		x[++i] = random( lx1+offset,lx2-offset );
		y[i] = random( 0+offset,ly1-offset );
		
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
		int[] sfx = { SF_X,SF_X-2*GRID_SIZE,SF_X-2*GRID_SIZE };
		int[] sfy = { SF_Y,SF_Y+TRACK_SIZE/2+1,SF_Y-TRACK_SIZE/2-1 };
		g.setColor( SF_COLOR );
		g.setStroke( new BasicStroke() );
		g.fillPolygon( sfx,sfy,3 );
	}
	
	private void newRace() {
		// init players
		for( int i = 0; i < MAX_PLAYERS; i++ ) {
			if ( i < (_h_n+_ai_n) ) {
				_p_x[i] = SF_X;
				_p_y[i] = SF_Y;
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
		_score_pos = 43;
		
		newOverlay();
		newTrace();

	}
	
	private void newTrace() {
		int x,y;
		
		// init 
		for ( x = 0; x < WIDTH; x++ ) {
			for ( y = 0; y < HEIGHT; y++ ) {
				Color color = new Color( _track.getRGB( x,y ) );
				_trace[x][y] = WALL;
				if ( color.equals( SF_COLOR ) ) { // start/finish
					_trace[x][y] = START_FINISH;
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
        
        currentPoints.add( new Point( SF_X-2*GRID_SIZE,SF_Y ) );
        _trace[SF_X-2*GRID_SIZE][SF_Y] = 1;
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

        _halfway = (_trace[SF_X+GRID_SIZE][SF_Y])/2;
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
		long t = _t;
		_t = _t0 + 2*(Calendar.getInstance().getTimeInMillis()-_tm0); // speed-up 2x
		
		if ( _t > _t0 + 1000 ) {
			_t = _t0 + 1000;
		}
		
		Graphics2D g = (Graphics2D)_overlay.getGraphics();
		for ( ; t <= _t; t += 10 ) {
			for( int i = 0; i < MAX_PLAYERS; i++ ) {
				if ( _p_x[i] != 0 ) {
					g.setColor( PLAYER_COLORS[i] );
			        int pixs = Math.max( Math.abs( _p_u[i] ), Math.abs( _p_v[i] ) );
					double dx = ( (double)_p_u[i] ) / ( (double)pixs );
					double dy = ( (double)_p_v[i] ) / ( (double)pixs );
					
					while( _t0 + (_p_i[i])*(1000.0/pixs) < _t ) {
	
						_p_i[i]++;
						_p_x[i] = _p_x0[i] + (int)Math.round( (_p_i[i])*dx );
						_p_y[i] = _p_y0[i] + (int)Math.round( (_p_i[i])*dy );
						
						g.fillRect( _p_x[i], _p_y[i], 1, 1 );
						
						//System.out.println( "trace " + _trace[_p_x[i]][_p_y[i]] );
						
						int v = _trace[_p_x[i]][_p_y[i]];
						
						// check halfway [todo, check 1/3, 2/3]
						if ( Math.abs( v-_halfway ) < 10 ) {
							_p_halfway[i] = true;
						}
											
						// check surface
						if ( v == WALL ) { // wall
							_p_x[i] = 0; // out of game
							//System.out.println( "p " + i + " hit wall" );
							break;
						}
						if ( v == START_FINISH && _p_halfway[i] ) { // start/finish && finished
							g.drawString( "player " + String.valueOf( i+1 ) + ": " + String.valueOf( t/1000.0 ), 5, _score_pos );
							_score_pos += 15;
							//System.out.println( "p " + i + " finished" );
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
	
	private void runAI( int x0, int y0, int u0, int v0, int du0, int dv0, int n ) {
		n--;
		int tv = 0;
		int du = -GRID_SIZE;
		
		for ( ; du < 2*GRID_SIZE; du += GRID_SIZE ) {
			for ( int dv = -GRID_SIZE; dv < 2*GRID_SIZE; dv += GRID_SIZE ) {
				if ( n == (AI_LEVEL-1) ) {
					du0 = du;
					dv0 = dv;
				}
				
				int u = u0+du;
				int v = v0+dv;
		        int pixs = Math.max( Math.abs( u ), Math.abs( v ) );
				double dx = ( (double)u ) / ( (double)pixs );
				double dy = ( (double)v ) / ( (double)pixs );
				boolean goOn = true;
				
				int x = x0;
				int y = y0;
				for ( int i = 1; i <= pixs; i++ ) {
					x = x0 + (int)Math.round( i*dx );
					y = y0 + (int)Math.round( i*dy );

					// check surface
					tv = _trace[x][y];
					if ( tv == WALL ) {
						goOn = false;
						break;
					}
					if ( tv == START_FINISH ) { // start/finish
						if ( _p_halfway[_currentPlayer] ) {
							int f = -n*100-pixs;
							if ( f < _tv ) {
								_tv = f;
								_du = du0;
								_dv = dv0;
							}
						}
						goOn = false;
						break;
					}
				}
				
				if ( goOn ) {
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
}