<?php
include("db_only_connect.php");
$tbl_name="users"; // Table name

// Define $email and $password
$email		= $_REQUEST['email'];
$password	= $_REQUEST['password'];
$name		= $_REQUEST['name'];

if($email && $password) {
    // To protect MySQL injection (more detail about MySQL injection)
    $email 		= stripslashes($email);
    $password 	= stripslashes($password);
    $name 		= stripslashes($name);
    $email 		= mysql_real_escape_string($email);
    $password 	= mysql_real_escape_string($password);
    $name 		= mysql_real_escape_string($name);
    $password 	= md5($password);

    // Check if user email is already present
    $sql	= "SELECT * FROM `$tbl_name` WHERE `email`='$email'";
    $result	= mysql_query($sql);
    if (!$result) {
        $message  = 'Invalid query: ' . mysql_error() . "\n";
        $message .= 'Whole query: ' . $query;
        die($message);
    }
    $count	= mysql_num_rows($result);
    if ($count == 1) {
        echo "Error: email already exists";
    } else {
        // Insert into database
        $sql = "INSERT INTO `$tbl_name` (`id`,`email`,`password`,`name`,`UUID`) VALUES (NULL ,'$email','$password','$name',UUID())";
        $result = mysql_query($sql);
        if(!$result)
        {
            $message  = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            die($message);
        }

        // Fetch userId
        $sql="SELECT * FROM `$tbl_name` WHERE `email`='$email'";
        $result = mysql_query($sql);
        $row = mysql_fetch_assoc($result);
        //$userId = $row['id'];
        //$_SESSION['userId'] = $userId;
	
        // Create tag for user
        $sql = "INSERT INTO `tags` (`id`,`tag`,`tagged_id`,`parent_id`,`type`,`date`) ";
        $sql .= "VALUES (NULL,'/$userId/','$userId','0','users',NOW())";
        $result = mysql_query($sql);
        if (!$result) {
            $message = 'Invalid query: ' . mysql_error() . "\n";
            $message .= 'Whole query: ' . $query;
            die($message);
        }
	include_once("login.php");
        echo "OK";
    }
}

?>
