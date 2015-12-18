<?php

header("Access-Control-Allow-Origin: *");

// error_reporting(E_ALL);
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 1);

require_once '../../../backend/db_handler.php';

// setUp Slim
require_once '../../../backend/libs/Slim/Slim.php';
\Slim\Slim::registerAutoloader();
\Slim\Route::setDefaultConditions(array(
    'userid' => '[0-9]+',
    'friendid' => '[0-9]+',
    'locationid' => '[0-9]+'
));

// setUp log4php
require_once '../../../backend/libs/log4php/Logger.php';
Logger::configure('log4php-config.xml');

$logger = Logger::getLogger("slim");
$app = new \Slim\Slim();
$userid = -1;

/**
 * Verifying required params posted or not
 */
function verifyRequiredParams($verifyRequiredParams) {
    $logger = $GLOBALS['logger'];
    $logger->debug("verifyRequiredParams: verifyRequiredParams: " . $verifyRequiredParams);
    $error = false;
    $error_fields = "";
    $request_params = array();
    $request_params = $_REQUEST;
    // Handling PUT request params
    if ($_SERVER['REQUEST_METHOD'] == 'PUT') {
        $app = \Slim\Slim::getInstance();
        parse_str($app->request()->getBody(), $request_params);
    }
    foreach ($required_fields as $field) {
        if (!isset($request_params[$field]) || strlen(trim($request_params[$field])) <= 0) {
            $error = true;
            $error_fields .= $field . ', ';
        }
    }

    if ($error) {
        // Required field(s) are missing or empty --> echo error json and stop the app
        $response = array();
        $app = \Slim\Slim::getInstance();
        $logger->debug('Required field(s) ' . substr($error_fields, 0, -2) . ' is missing or empty');
        echoErrorResponse(400, SUCCESS_REQUEST_ERROR, 'Required field(s) ' . substr($error_fields, 0, -2) . ' is missing or empty');
        $app->stop();
        exit();
    }
}

/**
 * Echoing json response to client
 * @param httpCode - String - Http response code
 * @param successResponse - ? - data content
 */
function echoSuccessResponse($httpCode, $successResponse) {
    $app = \Slim\Slim::getInstance();
    $app->status($httpCode); // Http response code
    $app->contentType('application/json'); // setting response content type to json
    $logger = $GLOBALS['logger'];
    $logger->debug("echoSuccessResponse: httpCode: " . $httpCode . " , response: " . $successResponse);
    echo json_encode(array("isSuccess" => true, "data" => $successResponse, "error" => null));
}

/**
 * Echoing json response to client
 * @param httpCode - int - Http response code
 * @param statusCode - int - statuscode 
 * @param message - String - errorcode
 */
function echoErrorResponse($httpCode, $statusCode, $message = "An Error :(") {
    $app = \Slim\Slim::getInstance();
    $app->status($httpCode);
    $app->contentType('application/json');
    $logger = $GLOBALS['logger'];
    $logger->debug("echoErrorResponse: httpCode: " . $httpCode . " , statusCode: " . $statusCode. " , message: " . $message);
    echo json_encode(array("isSuccess" => false, "data" => null, "error" => array("httpCode" => $statusCode, "message" => $message)));
}

/*
 * Validate a google oauth2 token
 * @Param token 
 * @Return the corresponding in plaintext
 *  or NULL if the Token is invalid
 */
function validateToken($token){
    $logger = $GLOBALS['logger'];
    $url = 'https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=' . $token;
    $response = file_get_contents($url);
    $info = json_decode($response, true);
    
    if (is_null($info['email'])) {
        return NULL;
    }
    
    $client_email = $info['email'];
    $client_id_web = $info['audience'];
    $client_id_android = $info['issued_to'];
    
    // echo $client_email . "<br/>";
    // echo $client_id_web . "<br/>";
    // echo $client_id_android . "<br/>";
    // echo '  ---  '. "<br/>";
    // echo '  ---  '. "<br/>";
    // echo $email . "<br/>";
    // echo CLIENT_ID_WEB . "<br/>";
    // echo CLIENT_ID_ANDROID_RELEASE . "<br/>";
    // echo CLIENT_ID_ANDROID_DEBUG . "<br/>";
    
    if ($client_id_web === CLIENT_ID_WEB && ($client_id_android === CLIENT_ID_ANDROID_RELEASE || $client_id_android === CLIENT_ID_ANDROID_DEBUG)) {
        $logger->debug("validateToken: " . $token . " is valid");
        return $client_email;
    } else { 
        $logger->debug("validateToken: " . $token . " is invalid");
        return NULL;
    }
    
}

function authenticate(\Slim\Route $route) {
    $logger = $GLOBALS['logger'];
    // $logger->debug("authenticate: " . $route.getPattern() . " " . $route.getHttpMethods() . " " . $route.getParams());
    $headers = apache_request_headers();
    if (isset($headers['token']) && strlen(trim($headers['token'])) > 0) {
        $token = $headers['token'];
        //print_r($token);
        $userEmail = validateToken($token);
        //print_r($userEmail);
        if ($userEmail !== NULL) {
            $con = new DBFunctions();
            $userid = $con->getIDfromEmail(hashString($userEmail));
            //print_r($userEmail);
            if ($userid === -1){ // Unknown User
                $logger->debug("Request from unknown User: " . $userEmail);
                echoErrorResponse(404, SUCCESS_REQUEST_ERROR, "Unknown User: " . $email);
                $app = \Slim\Slim::getInstance();
                $app->stop();
            } else { // -> pass
                $GLOBALS['user_id'] = $userid;
            }
        } else {
            $logger->debug("authenticate failed for: " . $userid . " " . $token);
            echoErrorResponse(401, SUCCESS_TOKEN_ERROR, "Invalid Token: " . $token);
            $app = \Slim\Slim::getInstance();
            $app->stop();
        }
    } else {
        $logger->debug("Authorization Header is missing: " . $userid);
        echoErrorResponse(400, SUCCESS_REQUEST_ERROR, "Authorization Header is missing");
        $app = \Slim\Slim::getInstance();
        $app->stop();
    }
    
}

/* 
 * REST Routes
 */ 

/*
 * display the REST API homepage in templates folder
 */
$app->get('/', function() use ($app, $logger) {
    $logger->info("Request: GET /");
    $app->render('index.html');
});

/*
$app->post('/:friendid', function($friendid) use ($app) {
    verifyRequiredParams(array('accepted'));
    $accepted = $app->request->post('accepted');
    $accepted2 = (bool) $accepted;
    $accepted3 = filter_var($accepted, FILTER_VALIDATE_BOOLEAN);
    print_r($friendid);
    print_r($accepted);
    print_r($accepted2);
    print_r($accepted3);
    echo 3 . $accepted3;
    
    echo "\n";
    var_dump($friendid);
    var_dump($accepted);
    var_dump($accepted2);
    var_dump($accepted3);
});
*/

// Version group v1
$app->group('/v1', function () use ($app, $logger) {

    // User group
    $app->group('/users', function () use ($app, $logger) {

        // create new user
        $app->post('/', function () use ($app, $logger) {
            $logger->info("Request: POST v1/users");
            $headers = apache_request_headers();
            if (isset($headers['token']) && strlen(trim($headers['token'])) > 0) {
                $token = $headers['token'];
                $userEmail = validateToken($token);
                // check for required params
                verifyRequiredParams(array('name', 'email', 'gcm_reg_id', 'plus_id'));
                try {
                    $con = new DBFunctions();
                    $res = $con->insertUser(
                        hashString($app->request->post('email')), 
                        $app->request->post('name'), 
                        $app->request->post('gcm_reg_id'), 
                        $app->request->post('plus_id')
                    );
                    echoSuccessResponse(201, $res);
                    $logger->info("insertUser returned successfully");
                } catch (PDOException $e) {
                    $logger.error("insertUser db PDOException", $e);
                    echoErrorResponse(500, $e->getCode(), $e->getMessage());
                }
            } else {
                echoErrorResponse(400, SUCCESS_REQUEST_ERROR, "Authorization Header is missing");
                $logger.error("insertUser Authorization Header is missing", $e);
            }
        });
        /*
        // Get user with ID
        $app->get('/:userid', 'authenticate', function ($userid) {
            echoErrorResponse(501, 501);
        });

        // Update user with ID
        $app->put('/:userid', 'authenticate', function ($userid) {
            echoErrorResponse(501, 501);
        });
        */
        // Delete user with ID
        $app->delete('/:userid', 'authenticate', function ($userid) use ($logger) {
            $logger->info("Request: DELETE v1/users/" . $userid);
            try {
                $con = new DBFunctions();
                $response = $con->deleteUser($userid);
                echoSuccessResponse(200, $response);
                $logger->info("deleteUser returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("deleteUser db PDOException", $e);
            }
        });
    });

    // Friend group
    $app->group('/friends', 'authenticate', function () use ($app, $logger) {

        // get all friends
        $app->get('/', function () use ($logger) {
           $logger->info("Request: GET /v1/friends");
           try {
                $con = new DBFunctions();
                $response = $con->getFriends($GLOBALS['user_id']);
                echoSuccessResponse(200, $response);
                $logger->info("getFriends returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("getFriends db PDOException", $e);
            }
        });
        
        // add a friend
        $app->post('/', function () use ($app, $logger){
            $logger->info("Request: POST /v1/friends");
            verifyRequiredParams(array('friendemail'));
            try {
                $con = new DBFunctions();
                $friend_id = $con->getIDfromEmail(hashString($app->request->post('friendemail')));
                if ($friend_id === -1){ // unknown friend mail
                    echoSuccessResponse(200, array("message" => "Friend: " . $app->request->post('friendemail') . " is not registered."));
                } else {
                    $response = $con->insertFriend($GLOBALS['user_id'], $friend_id);
                    echoSuccessResponse(200, $response);
                }
                $logger->info("insertFriend returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("insertFriend db PDOException", $e);
            }
        });

        // get all unconfirmed friends
        $app->get('/unconfirmed', function () use ($logger) {
            $logger->info("Request: GET /v1/friends/unconfirmed");
            try {
                $con = new DBFunctions();
                $response = $con->getNewFriends($GLOBALS['user_id']);
                echoSuccessResponse(200, $response);
                $logger->info("getNewFriends returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("getNewFriends db PDOException", $e);
            }
        });
               
        // poke all friends
        $app->get('/poke', function () use ($logger){
            $logger->info("Request: GET /v1/friends/poke");
            try {
                $con = new DBFunctions();
                $response = $con->sendGCM($con->getFriendRegIDsFromPersonId($GLOBALS['user_id']), GCM_POKE);
                echoSuccessResponse(200, $response);
                $logger->info("sendGCM returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("sendGCM db PDOException", $e);
            }
        });
        
        // confirm friend with ID
        $app->put('/:friendid', function ($friendid) use ($app, $logger){
            $logger->info("Request: PUT /v1/friends/" . $friendid);
            verifyRequiredParams(array('accepted'));
            try {
                $con = new DBFunctions();
                $response = $con->confirmFriend($GLOBALS['user_id'], $friendid, filter_var($app->request->put('accepted'), FILTER_VALIDATE_BOOLEAN));                
                echoSuccessResponse(200, $response);
                $logger->info("confirmFriend returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("confirmFriend db PDOException", $e);
            }
        }); 
        
        // delete friend with ID
        $app->delete('/:friendid', function ($friendid) use ($logger){
            $logger->info("Request: DELETE /v1/friends/" . $friendid);
            try {
                $con = new DBFunctions();
                $response = $con->deleteFriend($GLOBALS['user_id'], $friendid);
                echoSuccessResponse(200, $response);
                $logger->info("deleteFriend returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("deleteFriend db PDOException", $e);            }
        });
        
        // poke friend with ID
        $app->get('/:friendid/poke', function ($friendid) use ($logger){
           $logger->info("Request: GET /v1/friends/" . $friendid . "/poke");
           try {
                $con = new DBFunctions();
                $response = $con->sendGCM($con->getGCMIDsfromPersonID($friendid), GCM_POKE);
                echoSuccessResponse(200, $response);
                $logger->info("sendGCM returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("sendGCM db PDOException", $e);
            }
        });

    });
    
    // Device Group
    $app->group('/device', 'authenticate', function () use ($app, $logger) {
        // insert a new gcm regID
        $app->post('/', function () use ($app, $logger) {
            $logger->info("Request: POST /v1/device/");
            verifyRequiredParams(array('gcm_reg_id'));
            try {
                $con = new DBFunctions();
                $response = $con->renovate_gcm_id($GLOBALS['user_id'],  $app->request->post('gcm_reg_id'));
                echoSuccessResponse(200, $response);
                $logger->info("renovate_gcm_id returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("renovate_gcm_id db PDOException", $e);
            }
        });
    });
    
    // Location group
    $app->group('/locations', 'authenticate', function () use ($app, $logger) {

        // get all locations, + filter
        $app->get('/', function () use ($app) {
            echoErrorResponse(501, 501);
        });
        
        // insert a new Location
        $app->post('/', function () use ($app, $logger) {
            $logger->info("Request: POST /v1/locations/");
            verifyRequiredParams(array('latitude', 'longitude', 'altitude', 'accuracy', 'address', 'updated_on'));
            try {
                $con = new DBFunctions();
                $response = $con->updateLocation(
                    $GLOBALS['user_id'], 
                    $app->request->post('latitude'),
                    $app->request->post('longitude'),
                    $app->request->post('altitude'),
                    $app->request->post('accuracy'),
                    $app->request->post('address'),
                    $app->request->post('updated_on')                    
                 );
                echoSuccessResponse(200, $response);
                $logger->info("updateLocation returned successfully");
            } catch (PDOException $e) {
                echoErrorResponse(500, $e->getCode(), $e->getMessage());
                $logger.error("updateLocation db PDOException", $e);
            }
        });
        
        // get Location with ID
        $app->get('/:locationid', function ($locationid) use ($app) {
            echoErrorResponse(501, 501);
        });

        // update a Location with ID
        $app->put('/:locationid', function ($locationid) use ($app) {
            echoErrorResponse(501, 501);
        });

        // delete a Location with ID
        $app->delete('/:locationid', function ($locationid) use ($app) {
            echoErrorResponse(501, 501);
        });
        
    });
    
});

$app->run();

?>