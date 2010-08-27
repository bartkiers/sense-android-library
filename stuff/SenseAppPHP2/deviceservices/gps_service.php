<?php 
include_once("../db_connect.php");

//$lat = $_REQUEST['latitude'];
//$long = $_REQUEST['longitude'];
$ds_id		= $_REQUEST['ds_id'];
$ds_type	= $_REQUEST['ds_type'];
$device_id	= $_REQUEST['device_id'];
$noPitch	= $_REQUEST['noPitch'];

// get from the database the long lat
$positionSensorID = 14;
$sql = "select * from sensor_data where device_id='$device_id' and sensor_type='$positionSensorID' ORDER BY id DESC LIMIT 0,1";
$result = mysql_query($sql);	
if(!$result)			 
    die("error"); 
  $count	= mysql_num_rows($result);
  if($count == 0 )
    return;
  $row = mysql_fetch_assoc($result);
  $longLatValue = $row['sensor_value'];	
//echo "long lat: $longLatValue\n" ;
// find orientation
$orientationID = 14;
$sql = "select * from sensor_type where name='orientation'";
$result = mysql_query($sql);	
if(!$result)			 
    die("error"); 
$orientationStr = "";
while( $row = mysql_fetch_assoc($result))
{
    if(strlen($orientationStr) > 0)
      $orientationStr .= " or ";
      $orientationID = $row['id'];
    $orientationStr .= " sensor_type ='$orientationID' ";
}
$sql = "select * from sensor_data where device_id='$device_id' and ( $orientationStr ) ORDER BY id DESC LIMIT 0,1";
$result = mysql_query($sql);	
if(!$result)			 
    die("error");  
  $row = mysql_fetch_assoc($result);
  $orientationValue = $row['sensor_value'];	
//echo "orientation: $orientationValue\n";
// get the values out of json
$longLatJson = json_decode($longLatValue);
while(list($key, $value) = each($longLatJson))
  $$key = $value;

$orientationJson = json_decode($orientationValue);
while(list($key, $value) = each($orientationJson))
  $$key = $value;
$yaw = $azimuth;

//<meta http-equiv="refresh" content="5">
if($_REQUEST['getData'])
{
echo "<?xml version=\"1.0\" ?><root>
	<location> 
		  <longitude>$longitude</longitude>
		  <latitude>$latitude</latitude>
	</location>
	<orientation>
		    <pitch>$pitch</pitch>
		    <yaw>$yaw</yaw>
	</orientation>	
</root>";

}
else
{
  echo "lat:".$latitude." long:".$longitude."\n";
  echo "yaw:$yaw, pitch:$pitch";
?>
<html>
<head>
 <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;sensor=true&amp;key=ABQIAAAAhemGaS5PmIEPzvvfBHoE1RQHPyg5pH9x-vCl7Mtg7EdesbbpMRTozQtiOdyRBHcUwf7f5Sdsd-wW1Q" type="text/javascript"></script>
<script type="text/javascript">
var map;
var myPano;   
var panoClient;
var nextPanoId;
var myPOV;
    function initialize() {
	    panoClient = new GStreetviewClient();     
	    var theAlamo = new GLatLng(<?php echo "$latitude,$longitude"; ?>);
            map = new GMap2(document.getElementById("map_canvas"));
            map.setCenter(theAlamo, 13);
            map.setUIToDefault();
	    map.addOverlay(new GMarker(theAlamo));
	
	//  var fenwayPark = new GLatLng(51.89878,4.488473);
	myPOV = {<?php echo "yaw:$yaw, pitch:$pitch";?>};
	panoramaOptions = { latlng:theAlamo, pov:myPOV};
      myPano = new GStreetviewPanorama(document.getElementById("pano"), panoramaOptions);
      GEvent.addListener(myPano, "error", handleNoFlash);
     panoClient.getNearestPanorama(theAlamo, showPanoData);
    }
function loadNewData(yawVal, pitchVal, longitudeVal, latitudeVal)
{
<?php
if($noPitch==1)
    echo  "myPOV = {yaw:parseInt(yawVal)};\n";
else
  echo  "myPOV = {yaw:parseInt(yawVal), pitch:parseInt(pitchVal)};\n";
?>
  var theAlamo = new GLatLng(latitudeVal, longitudeVal);  
            map.setCenter(theAlamo, 13);
            map.setUIToDefault();
	    map.addOverlay(new GMarker(theAlamo));  
    myPano.setLocationAndPOV(theAlamo, myPOV);   
  panoClient.getNearestPanorama(theAlamo, showPanoData);
}
function showPanoData(panoData) {
 
//   nextPanoId = panoData.links[[0[].panoId;
//   var displayString = [[
//     "Panorama ID: " + panoData.location.panoId,
//     "LatLng: " + panoData.location.latlng,
//     "Copyright: " + panoData.copyright,
//     "Description: " + panoData.location.description,
//     "Next Pano ID: " + panoData.links[[0[].panoId[].join("");
  map.openInfoWindowHtml(panoData.location.latlng, displayString);
  myPano.setLocationAndPOV(panoData.location.latlng,myPOV);
}

   var http_request = false;
   function makeRequest(url, parameters) {
      http_request = false;
      if (window.XMLHttpRequest) { // Mozilla, Safari,...
         http_request = new XMLHttpRequest();
         if (http_request.overrideMimeType) {
            http_request.overrideMimeType('text/xml');
         }
      } else if (window.ActiveXObject) { // IE
         try {
            http_request = new ActiveXObject("Msxml2.XMLHTTP");
         } catch (e) {
            try {
               http_request = new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {}
         }
      }
      if (!http_request) {
         alert('Cannot create XMLHTTP instance');
         return false;
      }
      http_request.onreadystatechange = alertContents;
      http_request.open('GET', url + parameters, true);
      http_request.send(null);
   }

   function alertContents() {
      if (http_request.readyState == 4) {
         if (http_request.status == 200) {	    
            var xmldoc = http_request.responseXML;
	    // lat long
            var locationNode = xmldoc.documentElement.getElementsByTagName("location");

	    var latitude = locationNode[0].getElementsByTagName("latitude");	    
	    var latVal =  latitude[0].firstChild.nodeValue;

	    var longitude = locationNode[0].getElementsByTagName("longitude");
	    var longVal =  longitude[0].firstChild.nodeValue;
  
	    //orientation
	    var orientationNode = xmldoc.documentElement.getElementsByTagName("orientation");

	    var pitch = orientationNode[0].getElementsByTagName("pitch");	    
	    var pitchVal =  pitch[0].firstChild.nodeValue;

	    var yaw = orientationNode[0].getElementsByTagName("yaw");
	    var yawVal =  yaw[0].firstChild.nodeValue; 
	    loadNewData(yawVal, pitchVal, longVal, latVal);	   
         } else {
            alert('There was a problem with the request.'+http_request.status);
         }
      }
   }
   function do_xml() {
      setTimeout("do_xml()",2000);   
      makeRequest('gps_service.php', '?getData=1&device_id=<?php echo $device_id;?>');
   } 
do_xml();
</script>

</head>
<body onload="initialize()">
  <div id="map_canvas" style="width: 100%; height: 25%"></div>
<div id="pano" style="width: 100%; height: 75%"></div>
</body>
</html>
<?php 
}
?>