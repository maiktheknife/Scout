<?php

//error_reporting(E_ALL);
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 1);

require_once 'db_config.php';

function hashString($value){
    return hash("sha256", $value, FALSE);
}

class DBFunctions {
	private $con;
	
    function __construct() {
        $this->connect();
    }
	
	function __destruct() {
        $this->close();
    }
	
    function clear(){
        $this->con->beginTransaction();
        $this->con->query("truncate person cascade;");
        $this->con->commit();
    }
    
	private function connect() {
		// echo "connect <br />";
        try {
            $this->con = new PDO("pgsql:host=" . DB_SERVER . ";dbname=" . DB_DATABASE, DB_USER, DB_PASSWORD, array (PDO::ATTR_PERSISTENT => true, PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION));
        } catch (PDOException $e) {
            print "Error!: " . $e->getMessage() . "<br/>";
            throw $e;
        }           
    }
  
    private function close() {
		// echo "close <br />";
        $this->con = null;
    }
	
    /* User */
    		
	function insertUser ($email, $name, $gcm_regid, $plus_id){
         try {
            $person_id = $this->getIDfromEmail($email);
            if ($person_id !== -1) { // User already registered --> update the Name
                $this->con->beginTransaction();
                $stmt = $this->con->prepare("update person set name = :name where person_id = :person_id;");
                $stmt->bindParam(':name', $name);
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->execute();
                $this->con->commit();
                return array("message" => "User successfully updated.", "person_id" => $person_id);
            } else {  // insert a new user
                $this->con->beginTransaction();
                $stmt = $this->con->prepare("insert into person(name, email, plus_id) values (:name, :email, :plus_id);");
                $stmt->bindParam(':name', $name);
                $stmt->bindParam(':email', $email);
                $stmt->bindParam(':plus_id', $plus_id);
                $stmt->execute();
                $person_id = $this->con->lastInsertId('person_id_seq');
                
                $stmt = $this->con->prepare("insert into location(person_id, latitude, longitude, altitude, accuracy, address, updated_on) values (:person_id, 0, 0, 0, 0, null, current_timestamp);");
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->execute();
                
                $stmt = $this->con->prepare("insert into device(person_id, gcm_reg_id) values (:person_id, :gcm_reg_id);");
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->bindParam(':gcm_reg_id', $gcm_regid);
                $stmt->execute();
                $this->con->commit();
                
                return array("message" => "User successfully created.", "person_id" => $person_id);
            }        
        } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
	
    function deleteUser($person_id) {
         try {
            $this->con->beginTransaction();
            $stmt = $this->con->prepare("delete from person where person_id = :person_id;");
            $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
            $stmt->execute();
            $this->con->commit();
            return array("message" => "User successfully deleted.");
         } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
    }
  
    /* Friends */
  
    function getFriends($person_id){
        $stmt = $this->con->prepare("select f.friend_id person_id, u.name, u.email, u.plus_id, true as is_confirmed, l.latitude, l.longitude, l.altitude, l.accuracy, l.address, l.updated_on from v_person_person f inner join v_person u on u.person_id = f.friend_id inner join v_location_last l on u.person_id = l.person_id where f.person_id = :person_id and f.accepted = true;");
        $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
        $stmt->execute();
        $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        return array("friends" => $result);
    }
  
	function insertFriend ($person_id, $friend_id){
         try {
            if ($friend_id === -1) { // unknown friend --> error
                return -1;
            } else {
                $this->con->beginTransaction();
                $stmt = $this->con->prepare("insert into person_person (person_id, friend_id, accepted) VALUES (?, ?, false);");
                $stmt->bindParam(1, $person_id, PDO::PARAM_INT);
                $stmt->bindParam(2, $friend_id, PDO::PARAM_INT);
                $stmt->execute();
                $this->con->commit();
                return array("message" => "Friend successfully added.");
            }
        } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
	
    function getNewFriends($person_id){
        $stmt = $this->con->prepare("select u.person_id, u.name, u.email, u.plus_id from v_person u where u.person_id in (select person_id from v_person_person where friend_id = :person_id and accepted = false);");
        $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
        $stmt->execute();
        $result = $stmt->fetchAll(PDO::FETCH_ASSOC);
        return array("newfriends" => $result);
	}
    
    function confirmFriend($person_id, $friend_id, $accepted){
         try {
            $this->con->beginTransaction();
            if ($accepted === true) {
                // update old requests
                $stmt = $this->con->prepare("update person_person set accepted = true where person_id = :person_id and friend_id = :friend_id;");
                $stmt->bindParam(':person_id', $friend_id, PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $person_id, PDO::PARAM_INT);
                $stmt->execute();
                
                 // insert conter
                $stmt = $this->con->prepare("insert into person_person(friend_id, person_id, accepted) values (:friend_id, :person_id, true);");
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $friend_id, PDO::PARAM_INT);
                $stmt->execute();
            
            } else {
                // delete request
                $stmt = $this->con->prepare("delete from person_person where friend_id = :friend_id and person_id = :person_id;");
                $stmt->bindParam(':person_id', $friend_id, PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $person_id, PDO::PARAM_INT);
                $stmt->execute();
            }
            
            $this->con->commit();
            return array("message" => "Friend: " . $friend_id . " confirmed: " . $accepted);
         } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
    }
    
    /*
	function confirmFriends($person_id, $friendIDKommaList){
         try {
            $friendIDarray =  explode(",", $friendIDKommaList);
            $size = count($friendIDarray);
            $this->con->beginTransaction();
            
             // update old requests
            $stmt = $this->con->prepare("update person_person set accepted = true where person_id = :person_id and friend_id = :friend_id;");
            for($i=0; $i < $size; $i++) {
                $stmt->bindParam(':person_id', $friendIDarray[$i], PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $person_id, PDO::PARAM_INT);
                $stmt->execute();
            }
            
            // insert gegensatz
            $stmt = $this->con->prepare("insert into person_person(friend_id, person_id, accepted) values (:friend_id, :person_id, true);");
            for($i=0; $i < $size; $i++) {
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $friendIDarray[$i], PDO::PARAM_INT);
                $stmt->execute();
            }
            
            $this->con->query("delete from person_person where friend_id = " . $person_id . " and accepted is false;");
            $this->con->commit();
            return array("message" => "Friends confirmed");
         } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
    */
    
    function deleteFriend($person_id, $friend_id){
        return $this->deleteFriends($person_id, $friend_id);
    }
	
	private function deleteFriends($person_id, $friendIDKommaList){
        try {
            $friendsarray =  explode(",", $friendIDKommaList);
            $size = count($friendsarray);
            
            $this->con->beginTransaction();
            $stmt = $this->con->prepare("delete from person_person where (person_id = :person_id and friend_id = :friend_id) or (friend_id = :person_id and person_id = :friend_id);");
            for($i=0; $i < $size; $i++) {
                $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
                $stmt->bindParam(':friend_id', $friendsarray[$i], PDO::PARAM_INT);
                $stmt->execute();
            }
            $this->con->commit();
            return array("message" => "Friends deleted");
        } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
	
    function sendGCM($registration_ids, $message) {
        $response = array();
        $url = 'https://gcm-http.googleapis.com/gcm/send';
		
		if (count($registration_ids) === 0) {
			$response["success"] = SUCCESS_NO_ERROR;
			$response["message"] = "No GCM ID's found";
			return $response;
		}
		
		$headers = array(
            'Authorization: key=' . GOOGLE_GCM_API_KEY,
            'Content-Type: application/json'
        );
		
		$fields = array(
            'registration_ids' => $registration_ids,
            'data' => array("message" => $message)
        );
		
        // Open connection
        $ch = curl_init();
 
        // Set the url, number of POST vars, POST data
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
 
        // Disabling SSL Certificate support temporarly
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
 
        // Execute post
        $result = curl_exec($ch);
        
		 // Close connection
        curl_close($ch);
		
        if ($result === FALSE) {
			$response["success"] = SUCCESS_INTERNAL_ERROR;
			$response["message"] = $result;
			return $response;
        }
 
		$response["success"] = SUCCESS_NO_ERROR;
		$response["message"] = $result;
		return $response;
    }
	 
    /* Helper */
     
    function getIDfromEmail ($email){
        $stmt = $this->con->prepare("select person_id from v_person where email = :email;");
        $stmt->bindParam(':email', $email);
        $stmt->execute();
        $personid = -1;
        while ($row = $stmt->fetch(PDO::FETCH_NUM, PDO::FETCH_ORI_NEXT)) {
            $personid = $row[0];
        }
        return $personid;
	}
    
    function getGCMIDsfromPersonID ($person_id) {
        $stmt = $this->con->prepare("select gcm_reg_id from v_device where person_id = :person_id;");
        $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
        $stmt->execute();
        $result = $stmt->fetchAll(PDO::FETCH_COLUMN);
        return $result;
    }
	
	function getFriendRegIDsFromPersonId($person_id){
        $stmt = $this->con->prepare("select p.gcm_reg_id from v_device p inner join v_person u on p.person_id = u.person_id where u.person_id in (select person_id from v_person_person where friend_id = :person_id and accepted is true);");
        $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
        $stmt->execute();
        $result = $stmt->fetchAll(PDO::FETCH_COLUMN);
        return $result;
	}

    /* Device */
    
    function renovate_gcm_id($person_id, $gcm_reg_id) {
        try {
            $this->con->beginTransaction();
            $stmt = $this->con->prepare("insert into device (person_id, gcm_reg_id) values (:person_id, :gcm_reg_id);");
            $stmt->bindParam(':person_id', $person_id, PDO::PARAM_INT);
            $stmt->bindParam(':gcm_reg_id', $gcm_reg_id);
            $stmt->execute();
            $this->con->commit();
            return array ("message" => "GCM Reg ID renovated");
        } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
    
    /* Location */
    
    function updateLocation($person_id, $latitude, $longitude, $altitude, $accuracy, $address, $updated_on){
        try {
            $this->con->beginTransaction();
            $stmt = $this->con->prepare("insert into location (person_id, latitude, longitude, altitude, accuracy, address, updated_on) values (:userid, :latitude, :longitude, :altitude, :accuracy, :address, :updated_on);");
            $stmt->bindParam(':userid', $person_id, PDO::PARAM_INT);
            $stmt->bindParam(':latitude', $latitude);
            $stmt->bindParam(':longitude', $longitude);
            $stmt->bindParam(':altitude', $altitude);
            $stmt->bindParam(':accuracy', $accuracy);
            $stmt->bindParam(':address', $address);
            $stmt->bindParam(':updated_on', $updated_on);
            $stmt->execute();
            $this->con->commit();
            return array("message" => "Location updated");
        } catch (PDOException $e) {
            $this->con->rollBack();
            throw $e;
        }
	}
	
    /*
	function mergeProcessEntries($email, $entries){
		$query = "insert into v_location (person_id, latitude, longitude, accuracy, updated_on) values (?, ?, ?, ?, ?);";
		$response = array();
		
		$friendsarray = json_decode($entries, true);
		$size = count($friendsarray);
		$person_id = $this->getIDfromEmail($email);
		$stmt = $this->con->prepare($query);
		
		for($i=0; $i < $size; $i++) {
			$tmp = $friendsarray[$i];
			$latitude = $tmp["latitude"];
			$longitude= $tmp["longitude"];
			$accuracy = $tmp["accuracy"];
			$updated_on = $tmp["updated_on"];

			$stmt->bind_param('iddds', $person_id, $latitude, $longitude, $accuracy, $updated_on);
			$result = $stmt->execute();
			// echo $result;
		}
		$this->con->commit();
		
		$response["success"] = SUCCESS_NO_ERROR;
		$response["message"] = "Process inserted";
		return $response;
	}
	*/
    
}

?>