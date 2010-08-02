<?php
include_once("db_connect.php");
include_once("deviceID_check.php");
$tbl_name="sensor_data"; // Table name

$baseURL = "http://demo.almende.com/commonSense2/";
// Get input
$userId		= $_SESSION['userId'];
$sensorName	= $_REQUEST['sensorName'];
//$sensorValue	= $_REQUEST['sensorValue'];

 // begin Dave B's Q&D file upload security code 
  $allowedExtensions = array("txt","csv","htm","html","xml", 
    "css","doc","xls","rtf","ppt","pdf","swf","flv","avi", 
    "wmv","mov","jpg","jpeg","gif","png", "3gp", "mp4"); 
  foreach ($_FILES as $file) { 
    if ($file['tmp_name'] > '') { 
      if (!in_array(end(explode(".", 
            strtolower($file['name']))), 
            $allowedExtensions)) { 
       die($file['name'].' is an invalid file type!<br/>'. 
        '<a href="javascript:history.go(-1);">'. 
        '&lt;&lt Go Back</a>'); 
      } 
    } 
  } 

if ($_FILES["file"]["error"] > 0)
{
    	echo "Return Code: " . $_FILES["file"]["error"] . "<br />";
}
else
{
	echo "Upload: " . $_FILES["file"]["name"] . "\n";
	//echo "Type: " . $_FILES["file"]["type"] . "<br />";
	//echo "Size: " . ($_FILES["file"]["size"] / 1024) . " Kb<br />";
	//echo "Temp file: " . $_FILES["file"]["tmp_name"] . "<br />";

		
	$fileBase = substr($_FILES["file"]["name"], 0, strlen($_FILES["file"]["name"])-4);
	$filePath = "upload/";
	$filePrefix = $userId."_".$deviceId."_";
	$newLocation =   $filePath.$filePrefix.$_FILES["file"]["name"];
	move_uploaded_file($_FILES["file"]["tmp_name"],$newLocation);
	$sensorValue = $baseURL.$newLocation;
	//if(substr_count(strtolower($_FILES["file"]["name"]),".3gp") || substr_count(strtolower($_FILES["file"]["name"]),".mp4"))
	//{
		$newLocationWave = $filePath.$filePrefix.$fileBase.".mp3";	
		$ffmpegCommand = "ffmpeg -y -i ". $newLocation ." -ac 1 -acodec libmp3lame -ar 22050 -f wav ".$newLocationWave;
		echo exec($ffmpegCommand);		
		echo exec("rm -rf ".$newLocation);
		$sensorValue = $baseURL.$newLocationWave;
	//}
	
	// update the database
	if($sensorName && $sensorValue)
	{
		// To protect MySQL injection (more detail about MySQL injection)	
		$sensorName 		= stripslashes($sensorName);	
		//$sensorValue 		= stripslashes($sensorValue);

		// Check if the sensor exists	
		$sql	= "SELECT * FROM sensor_type WHERE name = '$sensorName'";
		$result	= mysql_query($sql);	
		$count	= mysql_num_rows($result);

		// Get sensorType	
		if($count == 1)	
		{
			$row 		= mysql_fetch_assoc($result);	
			$sensorType 	= $row['id'];
		}
		// Create new sensor type
		else
		{	
			$sql	= "INSERT INTO sensor_type (`id` ,`name` `data_type`) VALUES (NULL ,  '$sensorName', 'string')";
			$result	= mysql_query($sql);
			if($result)
			{
				// Get sensorType
				$sql		= "SELECT * FROM sensor_type WHERE name = '$sensorName'";
				$result		= mysql_query($sql);	
				$row 		= mysql_fetch_assoc($result);	
				$sensorType 	= $row['id'];
			}
			else
			{	
				$message  = 'Invalid query: ' . mysql_error() . "\n";
				$message .= 'Whole query: ' . $query;
				die($message);
			}			
		}

		// Check if the database already has an entry
		$sql	= "SELECT * FROM sensor_data WHERE sensor_value = '$sensorValue' and sensor_type = '$sensorType'";
		$result	= mysql_query($sql);	
		$count	= mysql_num_rows($result);

		// Get sensorType	
		if($count == 1)	
		{
			$row 		= mysql_fetch_assoc($result);	
			$sensorID 	= $row['id'];
			$time = microtime(true);
			$sql	= "UPDATE sensor_data set date ='".$time."' where sensor_value = '$sensorValue' and sensor_type = '$sensorType'";
			$result	= mysql_query($sql);	
			if($result)
				echo "OK";
		}		
		else
		{	
			
			// Insert into DB
			$time = microtime(true);
			$sql	= "INSERT INTO $tbl_name (`id` ,`device_id` ,`sensor_type` ,`sensor_value` ,`date`) VALUES (NULL ,  '$deviceId', '$sensorType',  '$sensorValue',  '$time')";
			$result	= mysql_query($sql);
			if($result)
				echo "OK";
			else
			{	
				$message  = 'Invalid query: ' . mysql_error() . "\n";
				$message .= 'Whole query: ' . $query;
				die($message);
			}
		}	
	
	}
	else
		echo "Error: no sensorName or sensorValue given";
	}
 
?>
