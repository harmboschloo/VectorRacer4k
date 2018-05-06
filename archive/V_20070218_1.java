/*
 * Vector Racer 4K 
 * By Harm Boschloo
 * Vector racer is a turn-based racing game best player against
 * friends. Use the arrow and enter keys to navigate and play. 
 * Press escape to return to the menu or exit.
 * todo:
 * dots input selection
 * corner curbs
 * check size for a=254; Color(a,a,a)
 * check why it won't run for some people
 * improve sim loop
 * check 4kminers game (for draw loop etc.)
 * red/white wall
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

/*
 * Class for Vector Racer 4k
 */
class V extends JFrame
{
	// Global constants
	private final int GRID_SIZE = 15;
	private final int MAX_PLAYERS = 10;

	private final int MENU_STATE = 1;
	private final int INPUT_STATE = 2;
	private final int NEW_TRACK_STATE = 3;
	private final int PROCESS_STEP_STATE = 4;
	private final int NEW_RACE_STATE = 5;
	
	private final int MENU_RACE = 30;
	private final int MENU_PLAYERS = 50;
	private final int MENU_AIS = 70;
	private final int MENU_AI_LEVEL = 90;
	private final int MENU_NEW_TRACK = 110;

	private final int WALL = -2;
	private final int START_FINISH = -1;
	
	// Global variables
	private int _state = NEW_TRACK_STATE;
	private int _h_n = 1; // number of human players
	private int _ai_n = 3; // number of ai players
	private int _ai_level = 2; // level of the ai
	private boolean[] _p_halfway = new boolean[MAX_PLAYERS]; // player halfway indicator
	private int _currentPlayer = 0; // current player
	private int _du = 0; // change of current player speed in x direction 
	private int _dv = 0; // change of current player speed in y direction 
	private long _t = 0; // time
	private int _menu = MENU_RACE; // current menu indicator position
	private int[][] _trace; // track trace array
	private int _tv; // best ai trace value
	private int _halfway; // halfway trace value
	
	/*
	 * Entry point.
	 */
	public static void main( String argv[] ) {
		new V();
	}

	/*
	 * Vector Racer 4k constructor
	 */
	private V()	{
		super( "VR4K" );
		
		// ##################################################
		// Local constants
		// ##################################################
		final int WIDTH = 800;
		final int HEIGHT = 500;

		final int TRACK_SIZE = 70;
		final int PLAYER_R = 4;

	    final int PROCESS_STATE = 6;
		final int AI_INPUT_STATE = 7;
		final int SIM_STATE = 8;
		
		final Color TRACK_COLOR = new Color( 90,90,90 );
		final Color SF_COLOR = new Color( 255,255,255 );
		final Color GRID_COLOR = new Color( 200,200,200,100 );

		/*final Color[] PLAYER_COLORS = {
        	new Color( random(150,255),random(50,255),random(150,255) ),
        	new Color( random(150,255),random(150,255),random(50,255) ),
        	new Color( random(50,255),random(150,255),random(150,255) ),
        	new Color( random(150,255),random(50,255),random(150,255) ),
        	new Color( random(150,255),random(150,255),random(50,255) ),
        	new Color( random(50,255),random(150,255),random(150,255) ),
        	new Color( random(150,255),random(50,255),random(150,255) ),
        	new Color( random(150,255),random(150,255),random(50,255) ),
        	new Color( random(50,255),random(150,255),random(150,255) ),
        	new Color( random(150,255),random(50,255),random(150,255) ),
	    };*/
		final Color[] PLAYER_COLORS = {
	        	new Color( 255,0,0 ),
	        	new Color( 255,255,0 ),
	        	new Color( 0,200,0 ),
	        	new Color( 255,100,255 ),
	        	new Color( 255,100,0 ),
	        	new Color( 255,255,255 ),
	        	new Color( 50,200,255 ),
	        	new Color( 255,200,100 ),
	        	new Color( 0,200,150 ),
	        	new Color( 150,255,100 ),
		};
		
		final  int SF_X = 450; // start/finish x position
		final  int SF_Y = 120; // start/finish y position

		// ##################################################
		// Local variables
		// ##################################################
		int[] p_x = new int[MAX_PLAYERS]; // player x position 
		int[] p_y = new int[MAX_PLAYERS]; // player y position 
		int[] p_x0 = new int[MAX_PLAYERS]; // player x position at start of simulation
		int[] p_y0 = new int[MAX_PLAYERS]; // player y position at start of simulation
		int[] p_u = new int[MAX_PLAYERS]; // player speed in x direction 
		int[] p_v = new int[MAX_PLAYERS]; // player speed in y direction 
		BufferedImage bg = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		BufferedImage track = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
		BufferedImage overlay = null;
		int[] p_i = new int[MAX_PLAYERS]; // player pixel count during simulation
		int message_pos = 0; // current message position
		double best = 0;

		long t0 = 0; // time at start of simulation
		long tm0 = 0; // time in millies at start of simulation

		//float dash_phase = 0.0f; // dash phase for input selection
		
		Graphics2D g; // graphics handle
		
		
		// ##################################################
		// Global variable initialization
		// ##################################################
		_trace = new int[WIDTH][HEIGHT];
		
		// ##################################################
		// Frame setup
		// ##################################################
		setSize( WIDTH+2,HEIGHT+26 );
		//setResizable( false );
		//setBackground( new Color( 0,0,0 ) );
		//setVisible( true );
		show();
		
		// draw buffer
		createBufferStrategy(2);
		BufferStrategy strategy = getBufferStrategy();
		
		// ##################################################
		// background
		// ##################################################
		g = (Graphics2D)bg.getGraphics();
		g.setColor( new Color( 0,0,0 ) );
		for ( int x = 0; x < WIDTH; x++ ) {
			for ( int y = 0; y < HEIGHT; y++ ) {
				g.setColor( new Color( random(190,210),random(190,210),random(140,160) ) );
				g.fillRect( x,y,1,1 );							
			}
		}
		
		/*g.setColor( new Color( random(150,255),random(150,255),random(150,255) ) );
		g.fillRect( 0,0,WIDTH,HEIGHT );
		for ( int i = 50; i > 0; --i ) {
			g.setColor( new Color( random(150,255),random(150,255),random(150,255),150 ) );
			int d = i*random(8,12)+75;
			g.fillRect( random(-d,WIDTH),random(-d,HEIGHT),d,d );
		}*/		
		
		// menu background
		g.setColor( new Color( 0,0,0,150 ) );
		g.fillRect( 0,0,110,HEIGHT );				
		
		// ##################################################
		// main loop
		// ##################################################
		while( true ) {

			// ##################################################
			// new track generation
			// ##################################################
			if ( _state == NEW_TRACK_STATE ) {

				// new overlay
				overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );

				// reset best score
				best = 1000;
				
				// create track
				final int offset_right = 120;
				final int offset = TRACK_SIZE/2+2;
				final int ly1 = SF_Y+offset;
				final int ly2 = SF_Y+3*offset;
				final int lx2 = SF_X-2*GRID_SIZE-offset;
				final int lx3 = SF_X+offset;
				final int lx1 = (lx2-offset_right)/2+offset_right;
				final int lx4 = lx3+(WIDTH-lx3)/2;
				//System.out.println( "lx1 " + lx1 );
				//System.out.println( "lx2 " + lx2 );
				//System.out.println( "lx3 " + lx3 );
				//System.out.println( "lx4 " + lx4 );
				//System.out.println( "ly1 " + ly1 );
				//System.out.println( "ly2 " + ly2 );
				final int[] x = {
					lx2,
					lx3,
					// top right
					random( lx3+offset,lx4-offset ),
					random( lx4,WIDTH-offset-10 ),
					// bottom right
					random( lx4+offset,WIDTH-offset-10 ),
					random( lx3+offset,lx4-offset ),
					// bottom middle
					random( lx2+offset,lx3-offset ),
					random( lx2+offset,lx3-offset ),
					// bottom left
					random( offset_right+offset,lx2-offset ),
					random( offset_right+offset,lx1-offset ),
					// top left
					random( offset_right+offset,lx1-offset ),
					random( lx1+offset,lx2 ),
				};
				final int[] y = {
					SF_Y,
					SF_Y,
					// top right
					random( 10+offset,ly2-offset ),
					random( 10+offset,ly1-offset ),
					// bottom right
					random( ly2+offset,HEIGHT-offset-10 ),
					random( ly2+offset,HEIGHT-offset-10 ),
					// bottom middle
					random( ly2+offset,HEIGHT-offset-10 ),
					random( ly1+offset,HEIGHT-offset-10 ),
					// bottom left
					random( ly2+offset,HEIGHT-offset-10 ),
					random( ly1+offset,ly2-offset ),
					// top left
					random( 10+offset,ly1-offset ),
					random( 10+offset,ly1-offset ),		
				};
				final int N = 12;
							
				// track graphics 
				g = (Graphics2D)track.getGraphics();
				
				// background
				g.drawImage( bg, 0, 0, null );
				
				// draw track shadow
				/*g.setColor( new Color( 0,0,0,80 ) );
				g.setStroke( new BasicStroke( TRACK_SIZE+25,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
				g.drawPolygon( x,y,N );*/

				// draw lines/curbs
				g.setColor( new Color( 254,254,254 ) );
				g.setStroke( new BasicStroke( TRACK_SIZE+4,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND ) );
				g.drawPolygon( x,y,N );
				
				final float[] DASH = { 6 };
				g.setColor( new Color( 254,0,0 ) );
				g.setStroke( new BasicStroke(
					TRACK_SIZE+4,
	                BasicStroke.CAP_BUTT, 
	                BasicStroke.JOIN_ROUND, 
	                0, 
	                DASH, 
	                0 ) );
				g.drawPolygon( x,y,N );
				
				g.setStroke( new BasicStroke(
						5,
		                BasicStroke.CAP_BUTT, 
		                BasicStroke.JOIN_ROUND, 
		                0, 
		                DASH, 
		                0 ) );
				for ( int i = 0; i < N; i++ ) {
					g.drawOval( x[i]-(TRACK_SIZE+2)/2,y[i]-(TRACK_SIZE+2)/2,TRACK_SIZE+2,TRACK_SIZE+2 );
				}
				
				g.setColor( new Color( 254,254,254 ) );
				g.setStroke( new BasicStroke(
						5,
		                BasicStroke.CAP_BUTT, 
		                BasicStroke.JOIN_ROUND, 
		                0, 
		                DASH, 
		                5 ) );
				for ( int i = 0; i < N; i++ ) {
					g.drawOval( x[i]-(TRACK_SIZE+2)/2,y[i]-(TRACK_SIZE+2)/2,TRACK_SIZE+2,TRACK_SIZE+2 );
				}
				
				// draw track
				g.setColor( TRACK_COLOR );
				g.setStroke( new BasicStroke( TRACK_SIZE,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND ) );
				g.drawPolygon( x,y,N );
				
				// draw start/finish
				final int[] sfx = { SF_X,SF_X-2*GRID_SIZE,SF_X-2*GRID_SIZE };
				final int[] sfy = { SF_Y,SF_Y+TRACK_SIZE/2+1,SF_Y-TRACK_SIZE/2-1 };
				g.setColor( SF_COLOR );
				g.setStroke( new BasicStroke() );
				g.fillPolygon( sfx,sfy,3 );
				
				// init trace & draw grid & draw bg
				for ( int xx = 0; xx < WIDTH; xx++ ) {
					for ( int yy = 0; yy < HEIGHT; yy++ ) {
						Color color = new Color( track.getRGB( xx,yy ) );
						_trace[xx][yy] = WALL;
						if ( color.equals( SF_COLOR ) ) { // start/finish
							_trace[xx][yy] = START_FINISH;
							int c = random( 230,250 );
							g.setColor( new Color( c,c,c ) );
							g.fillRect( xx,yy,1,1 );
							if ( (double)xx%(double)GRID_SIZE == 0.0 || (double)yy%(double)GRID_SIZE == 0.0 ) {
			                	g.setColor( GRID_COLOR );
			                	g.fillRect( xx,yy,1,1 );
			                }
						}
						if ( color.equals( TRACK_COLOR ) ) {
							_trace[xx][yy] = 0;
							final int c = random( 80,100 );
							g.setColor( new Color( c,c,c ) );
							g.fillRect( xx,yy,1,1 );
			                if ( (double)xx%(double)GRID_SIZE == 0.0 || (double)yy%(double)GRID_SIZE == 0.0 ) {
			                	g.setColor( GRID_COLOR );
			                	g.fillRect( xx,yy,1,1 );
			                }					
						}

					}
				}
				_state = MENU_STATE;
			}
			
			// ##################################################
			// set up new race
			// ##################################################
			if ( _state == NEW_RACE_STATE ) {
				_state = PROCESS_STATE;

				// init players
				for( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( i < (_h_n+_ai_n) ) {
						p_x[i] = SF_X;
						p_y[i] = SF_Y;
					}
					else {
						p_x[i] = 0;
						p_y[i] = 0;
					}
					p_u[i] = 0;
					p_v[i] = 0;
					_p_halfway[i] = false;
				}
				
				// misc inits
				_currentPlayer = 0;
				_t = 0;
				message_pos = 170;

				// new trace
		        Vector<Point> currentPoints = new Vector<Point>();
		        Vector<Point> changedPoints = new Vector<Point>();
		        int i,d,lx,ly,lv;
		        int[] dx = { -1,1,0,0 };
		        int[] dy = { 0,0,-1,1 };  
		        //int[] dx = { -1,1, 0,0,-1,1,-1, 1 };
		        //int[] dy = {  0,0,-1,1,-1,1, 1,-1 };
		        int x,y;
		        
		        currentPoints.add( new Point( SF_X-2*GRID_SIZE-1,SF_Y ) );
		        _trace[SF_X-2*GRID_SIZE-1][SF_Y] = 1;
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
		        
		        // new overlay
				overlay = new BufferedImage( WIDTH,HEIGHT,BufferedImage.TYPE_INT_ARGB );
				
				// let's race!
				/*g = (Graphics2D)overlay.getGraphics();
				g.drawString( "go!", 10, message_pos );
				message_pos += 15;*/
				
				// player input
				if ( _currentPlayer < _h_n ) {
					_state = INPUT_STATE;			 
				}
				else {
					_state = AI_INPUT_STATE;
				}
			}
			
			// ##################################################
			// do some drawing
			// ##################################################
			
			g = (Graphics2D)strategy.getDrawGraphics();
			g.translate( 1,25 );
			
			// track
			g.drawImage( track, 0, 0, null );
			
			// players
			if ( _state != MENU_STATE ) {
				for ( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( p_x[i] != 0 ) {
						g.setColor( PLAYER_COLORS[i] );
						g.drawOval( p_x[i]-PLAYER_R,p_y[i]-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
					}
				}
			}
				
			// overlay
			g.drawImage( overlay, 0, 0, null );

			// main menu
			if ( _state == MENU_STATE || _state == NEW_TRACK_STATE ) {
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawOval( 15,_menu-8,8,8 );
				g.drawString( "race",28,MENU_RACE );
				g.drawString( "players: " + String.valueOf( _h_n ),28,MENU_PLAYERS );
				g.drawString( "ais: " + String.valueOf( _ai_n ),28,MENU_AIS );
				g.drawString( "ai level: " + String.valueOf( (_ai_level-1) ),28,MENU_AI_LEVEL );
				g.drawString( "new track",28,MENU_NEW_TRACK );
			}
			else {
				// time
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawString( "time: " + String.valueOf( _t/1000.0 ),20,30 );
			}
			
			// best time
			if ( best < 1000 ) {
				g.setColor( PLAYER_COLORS[MAX_PLAYERS-1] );
				g.drawString( "best: " + String.valueOf( best ),10,150 );
			}
			
			// ##################################################
			// draw input selection
			// ##################################################
			if ( _state == INPUT_STATE ) {
				g.setColor( PLAYER_COLORS[_currentPlayer] );
				g.drawString( "player: " + String.valueOf( _currentPlayer+1 ),20,50 );
				g.fillOval( p_x[_currentPlayer]-PLAYER_R-1,p_y[_currentPlayer]-PLAYER_R-1,2*PLAYER_R+2,2*PLAYER_R+2 );
				
				// drive line
				g.drawLine( 
					p_x[_currentPlayer],
					p_y[_currentPlayer],
					p_x[_currentPlayer]+p_u[_currentPlayer]+_du,
					p_y[_currentPlayer]+p_v[_currentPlayer]+_dv );

				// acceleration line
				/*float[] dash = { 3.0f, 3.0f };
				g.setStroke( new BasicStroke(
					2.0f,
	                BasicStroke.CAP_BUTT, 
	                BasicStroke.JOIN_ROUND, 
	                0.0f, 
	                dash, 
	                dash_phase ) );
				dash_phase -= 0.5;
				if ( dash_phase < 0 ) { dash_phase += 6.0f; }
				g.drawLine( 
					p_x[_currentPlayer]+p_u[_currentPlayer],
					p_y[_currentPlayer]+p_v[_currentPlayer],
					p_x[_currentPlayer]+p_u[_currentPlayer]+_du,
					p_y[_currentPlayer]+p_v[_currentPlayer]+_dv );*/
				
				// input options
				g.setColor( new Color( 
					PLAYER_COLORS[_currentPlayer].getRed(),
					PLAYER_COLORS[_currentPlayer].getGreen(),
					PLAYER_COLORS[_currentPlayer].getBlue(),
					100 ) );
				/*final float[] DASH = { 1 };
				g.setStroke( new BasicStroke(
					1,
	                BasicStroke.CAP_BUTT, 
	                BasicStroke.JOIN_ROUND, 
	                0, 
	                DASH, 
	                0 ) );*/
				for ( int dx = -1; dx < 2; dx++ ) {
					for ( int dy = -1; dy < 2; dy++ ) {
						int x = p_x[_currentPlayer]+p_u[_currentPlayer]+GRID_SIZE*dx;
						int y = p_y[_currentPlayer]+p_v[_currentPlayer]+GRID_SIZE*dy;;
						g.drawOval( x-PLAYER_R,y-PLAYER_R,2*PLAYER_R,2*PLAYER_R );
					}
				}
				
				// current velocity line
				/*g.setStroke( new BasicStroke(
						1.0f,
		                BasicStroke.CAP_BUTT, 
		                BasicStroke.JOIN_ROUND, 
		                0.0f, 
		                dash, 
		                0.0f ) );
				g.drawLine( 
					p_x[_currentPlayer],
					p_y[_currentPlayer],
					p_x[_currentPlayer]+p_u[_currentPlayer],
					p_y[_currentPlayer]+p_v[_currentPlayer] );*/
			}
			
			// ##################################################
			// step processing
			// ##################################################
			if ( _state == PROCESS_STEP_STATE ) {
				
				_state = PROCESS_STATE;
				p_u[_currentPlayer] += _du;
				p_v[_currentPlayer] += _dv;
				
				_du = 0;
				_dv = 0;
				
				// next player
				while( true ) {
					_currentPlayer++;
					if ( (_h_n+_ai_n) == _currentPlayer ) {
						_currentPlayer = 0;

						for( int i = 0; i < MAX_PLAYERS; i++ ) {
							p_i[i] = 0;
							p_x0[i] = p_x[i];
							p_y0[i] = p_y[i];
						}
						t0 = _t;
						tm0 = Calendar.getInstance().getTimeInMillis( );
						
						_state = SIM_STATE;
						break;
					}
					else if ( p_x[_currentPlayer] != 0 ) {
						 if ( _currentPlayer < _h_n ) {
							 _state = INPUT_STATE;			 
						 }
						 else {
							 _state = AI_INPUT_STATE;
						 }
						break;
					}
				}
			}
			
			
			// ##################################################
			// do simulation
			// ##################################################
			if ( _state == SIM_STATE ) {

				// current time
				long t = _t;
				_t = t0 + 2*(Calendar.getInstance().getTimeInMillis()-tm0); // speed-up 2x
				
				if ( _t > t0 + 1000 ) {
					_t = t0 + 1000;
				}
				
				// run untill current time reached
				g = (Graphics2D)overlay.getGraphics();
				for ( ; t <= _t; t += 10 ) {
					for( int i = 0; i < MAX_PLAYERS; i++ ) {
						if ( p_x[i] != 0 ) {
							int pixs = Math.max( Math.abs( p_u[i] ), Math.abs( p_v[i] ) );
							double dx = ( (double)p_u[i] ) / ( (double)pixs );
							double dy = ( (double)p_v[i] ) / ( (double)pixs );
							
							while( t0 + 1000.0*(p_i[i]/(double)pixs) < _t ) {
			
								p_i[i]++;
								p_x[i] = p_x0[i] + (int)Math.round( (p_i[i])*dx );
								p_y[i] = p_y0[i] + (int)Math.round( (p_i[i])*dy );
								
								g.setColor( new Color( 
									PLAYER_COLORS[i].getRed(),
									PLAYER_COLORS[i].getGreen(),
									PLAYER_COLORS[i].getBlue(),
									10 ) );
								g.fillOval( p_x[i]-PLAYER_R, p_y[i]-PLAYER_R, 2*PLAYER_R, 2*PLAYER_R );
								
								
								int v = _trace[p_x[i]][p_y[i]];
								//System.out.println( "trace value: " + v );
								
								// check halfway [todo, check 1/3, 2/3]
								if ( Math.abs( v-_halfway ) < 10 ) {
									_p_halfway[i] = true;
								}
													
								// check surface
								g.setColor( PLAYER_COLORS[i] );
								if ( v == WALL ) { // wall
									g.drawString( "player " + String.valueOf( i+1 ) + " crashed", 10, message_pos );
									message_pos += 15;
									p_x[i] = 0; // out of game
									//System.out.println( "p " + i + " hit wall" );
									break;
								}
								if ( v == START_FINISH && _p_halfway[i] ) { // start/finish && finished
									double time = Math.round( t0 + 1000.0*(p_i[i]/(double)pixs) )/1000.0;
									if ( time < best ) { best = time; }
									g.drawString( "player " + String.valueOf( i+1 ) + ": " + String.valueOf( time ) , 10, message_pos );
									message_pos += 15;
									//System.out.println( "p " + i + " finished" );
									p_x[i] = 0; // out of game
									break;
								}
							}
						}
					}
				}
				
				// check how many players are still racing
				int n = 0;
				for( int i = 0; i < MAX_PLAYERS; i++ ) {
					if ( p_x[i] != 0 ) { 
						n++;
					}
				}
				
				if ( n == 0 ) { // no players racing, return to main menu
					/*g.drawString( "finished", 10, message_pos );
					message_pos += 15;*/
					
					_state = MENU_STATE;
				}
				else if ( _t == t0+1000 ) { // simulation step finished
					// find next player
					while ( p_x[_currentPlayer] == 0 ) {
						_currentPlayer++;
					}
					// change to (ai) input state 
					if ( _currentPlayer < _h_n ) {
						_state = INPUT_STATE;			 
					}
					else {
						_state = AI_INPUT_STATE;
					}
				}

			
			}
			
			// ##################################################
			// run ai
			// ##################################################
			if ( _state == AI_INPUT_STATE ) {
				_state = PROCESS_STATE;
				_tv = Integer.MAX_VALUE;
				runAI( 
					p_x[_currentPlayer], 
					p_y[_currentPlayer],
					p_u[_currentPlayer], 
					p_v[_currentPlayer],
					0,
					0,
					_ai_level );
				
				_state = PROCESS_STEP_STATE;
			}
			
			// ##################################################
			// show
			// ##################################################
			strategy.show();
			if ( !isVisible() ) {
				System.exit( 0 );
			}
			
			// ##################################################
			// sleep for a short while
			// ##################################################
			try {
				Thread.sleep( 10 );
			}
			catch( Exception e ) {
			}
		}
	}
	
	/*
	 * Key events
	 * @see java.awt.Component#processKeyEvent(java.awt.event.KeyEvent)
	 */
	protected void processKeyEvent( KeyEvent e ) {
		if ( e.getID() == KeyEvent.KEY_PRESSED ) {
		
			// ##################################################
			// player selection
			// ##################################################
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
					_state = PROCESS_STEP_STATE;				
				}
			}	
			
			// ##################################################
			// main menu
			// ##################################################
			if ( _state == MENU_STATE ) {
				// up
				if ( e.getKeyCode() == KeyEvent.VK_UP ) {
					if ( _menu == MENU_RACE ) {
						_menu = MENU_NEW_TRACK;
					}
					else {
						_menu -= 20;
					}
				}
				// down
				if ( e.getKeyCode() == KeyEvent.VK_DOWN ) {
					if ( _menu == MENU_NEW_TRACK ) {
						_menu = MENU_RACE;
					}
					else {
						_menu += 20;
					}
				}	
				// right
				if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
					if ( _menu == MENU_PLAYERS && (_h_n+_ai_n) < MAX_PLAYERS ) {
						_h_n++;
					}
					if ( _menu == MENU_AIS && (_h_n+_ai_n) < MAX_PLAYERS ) {
						_ai_n++;
					}
					if ( _menu == MENU_AI_LEVEL && _ai_level < 4 ) {
						_ai_level++;
					}
				}
				// left
				if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
					if ( _menu == MENU_PLAYERS && _h_n > 0 ) {
						_h_n--;
					}
					if ( _menu == MENU_AIS && _ai_n > 0 ) {
						_ai_n--;
					}
					if ( _menu == MENU_AI_LEVEL && _ai_level > 2 ) {
						_ai_level--;
					}
				}
				// enter
				if ( e.getKeyCode() == KeyEvent.VK_ENTER ) {
					if ( _menu == MENU_RACE ) {
						_state = NEW_RACE_STATE;
					}
					
					if ( _menu == MENU_NEW_TRACK ) {
						_state = NEW_TRACK_STATE;
					}
				}
				// escape
				if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
				{
					System.exit( 0 );
				}
			}
			
			// ##################################################
			// return to main menu
			// ##################################################
			if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
			{
				_state = MENU_STATE;
			}
		}
	}

	/*
	 * Runs the ai
	 */
	private void runAI( int x0, int y0, int u0, int v0, int du0, int dv0, int n ) {
		n--; // 1 iteration level deeper
		int tv = 0; // local trace value
		
		// checks every possible move and stores the best move (with the 'best' trace value)
		for ( int du = -GRID_SIZE; du < 2*GRID_SIZE; du += GRID_SIZE ) {
			for ( int dv = -GRID_SIZE; dv < 2*GRID_SIZE; dv += GRID_SIZE ) {
				// store move at first iteration
				if ( n == (_ai_level-1) ) {
					du0 = du;
					dv0 = dv;
				}
				
				// variables
				int u = u0+du;
				int v = v0+dv;
		        int pixs = Math.max( Math.abs( u ), Math.abs( v ) );
				double dx = ( (double)u ) / ( (double)pixs );
				double dy = ( (double)v ) / ( (double)pixs );
				boolean goOn = true;
				int x = x0;
				int y = y0;

				// check pixels for move
				for ( int i = 1; i <= pixs; i++ ) {
					x = x0 + (int)Math.round( i*dx );
					y = y0 + (int)Math.round( i*dy );

					// check surface
					tv = _trace[x][y];
					if ( tv == WALL ) { // hit a wall
						goOn = false;
						break;
					}
					if ( tv == START_FINISH ) { // start/finish
						if ( _p_halfway[_currentPlayer] ) { // finish reached
							// negavite value will always be better than regular value
							// random to spread out ai's at finish
							int f = -n*100-pixs+random( -1,1 );
							if ( f < _tv ) {
								_tv = f;
								_du = du0;
								_dv = dv0;
							}
						}
						goOn = false; // no need to look further
						break;
					}
				}
				
				// keep 'speed' of ai's at more or less exceptable level
				// if speed to high, no need to go on
				if ( _ai_level < 3 && pixs > random(3,4)*GRID_SIZE ) { goOn = false; }
		        if ( pixs > (_ai_level+2)*GRID_SIZE ) { goOn = false; }
		        if ( pixs > (_ai_level+random(1,2))*GRID_SIZE ) { goOn = false; }
				
				if ( goOn ) {
					if ( n > 0 ) { // next iteration level
						runAI( x,y,u,v,du0,dv0,n );
					}
					else { // last iteration level reached
						// check trace value
						if ( tv > 0 ) {
							// introduces some randomness in the first 3/4 of the track
							if ( tv > _halfway/2 ) {
								int rndm = GRID_SIZE-2*_ai_level;
								tv += random( -rndm,rndm ); 
							}
							// check best trace value and set choosen input
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
	
	/*
	 * Random int values between (and including) minimum and maximum
	 */
	private int random( int min, int max ) {
		return min + (int)Math.round( (max-min)*Math.random() );
	}
}