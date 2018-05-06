<? 
header( 'Content-type: application/x-java-jnlp-file' ); 
print( '<?xml version="1.0" encoding="UTF-8"?>' );
?>

<jnlp
  spec="1.0+"
  codebase="http://www.boschloo.net/vectorracer/4k/"
  href="vr4k.php">
  <information>
    <title>Vector Racer 4k</title>
    <vendor>Harm Boschloo</vendor>
    <homepage href="http://www.boschloo.net/vectorracer/?id=4k"/>
    <description>
	Vector racer is a turn-based racing game best played against friends. 
	Use the arrow and enter keys to navigate and play. 
	Press escape to return to the menu or exit.
	More info on www.boschloo.net/vectorracer/?id=4k.
	</description>
    <icon href="vr4k.gif"/>
  </information>
  <resources>
    <j2se version="1.4+"/>
    <jar href="vr4k.jar"/>
  </resources>
  <application-desc main-class="V"/> 
</jnlp> 