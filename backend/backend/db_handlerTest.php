<?php

error_reporting(E_ALL);
ini_set('display_errors', 1);

require_once 'db_handler.php';

class DBFunctionsTest extends PHPUnit_Framework_TestCase {
    private $db;
    
	public function setUp(){
        $this->db = new DBFunctions();
        $this->db->clear();
    }
    
    public function tearDown(){
        $this->db->clear();
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertUserNull() {
        $this->db->insertUser(null,null,null,null);
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertUserMailIsNull() {
        $this->db->insertUser(null, 'PHPUnitTest', 'ID', 'ID');
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertUserNullName() {
        $this->db->insertUser(hashString('testUser@mail.com'),null, 'ID', 'ID');
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertUserGCMIDIsNull() {
        $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', null, 'ID');
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertUserPlusIDIsNull() {
        $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', null);
    }
    
    function testInsertDeleteUser() {
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
        
        $result = $this->db->deleteUser($result['person_id']);
        $this->assertNotNull($result);
        $this->assertNotNull($result['message']); 
    }
    
    function testInsertUserDuplicate() {
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
        
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
    }
    
    function testGetIDfromEmail(){
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
        
        $result2 = $this->db->getIDfromEmail(hashString('testUser@mail.com'));
        $this->assertNotNull($result2);
        $this->assertEquals($result2, $result['person_id']);
        
        $result = $this->db->getIDfromEmail(hashString('wrongUser@email.com'));
        $this->assertNotNull($result);
        $this->assertEquals($result, -1);
    }
    
    function testInsertAddConfirmDeleteFriend() {
        // insert user
        $result1 = $this->db->insertUser(hashString('user@mail.com'), 'PHPUnitTest', 'ID1', 'ID_1');
        $this->assertNotNull($result1);
        $this->assertTrue(is_array($result1));
        $this->assertNotNull($result1['person_id']);
        
        $result2 = $this->db->insertUser(hashString('friend@mail.com'), 'PHPUnitTest', 'ID2', 'ID_2');
        $this->assertNotNull($result2);
        $this->assertTrue(is_array($result2));
        $this->assertNotNull($result2['person_id']);
        
        $result3 = $this->db->insertUser(hashString('friend2@mail.com'), 'PHPUnitTest', 'ID3', 'ID_3');
        $this->assertNotNull($result3);
        $this->assertTrue(is_array($result3));
        $this->assertNotNull($result3['person_id']);
        
        $result4 = $this->db->insertUser(hashString('friend3@mail.com'), 'PHPUnitTest', 'ID4', 'ID_4');
        $this->assertNotNull($result4);
        $this->assertTrue(is_array($result4));
        $this->assertNotNull($result4['person_id']);
        
        // insert friends        
        $result = $this->db->insertFriend($result1['person_id'], $result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->insertFriend($result1['person_id'], $result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->insertFriend($result1['person_id'], $result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->insertFriend($result2['person_id'], $result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        // get these friends request
        $result = $this->db->getNewFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 0);

        $result = $this->db->getNewFriends($result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 1);
        
        $result = $this->db->getNewFriends($result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 1);
        
        $result = $this->db->getNewFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 2);
        
        // confirm the Friends
        $result = $this->db->confirmFriend($result2['person_id'], $result1['person_id'], true);
        $this->assertNotNull($result);
        $this->assertNotNull($result['message']);
        
        $result = $this->db->confirmFriend($result3['person_id'], $result1['person_id'], true);
        $this->assertNotNull($result);
        $this->assertNotNull($result['message']);
        
        $result = $this->db->confirmFriend($result4['person_id'], $result1['person_id'], true);
        $this->assertNotNull($result);
        $this->assertNotNull($result['message']);
        
        // get friend info
        $result = $this->db->getFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 3);
        
        $result = $this->db->getFriends($result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 1);
        
        $result = $this->db->getFriends($result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 1);
        
        $result = $this->db->getFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 1);
        
        // be sure no more unconfirmed friends are there
        $result = $this->db->getNewFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 0);
        
        $result = $this->db->getNewFriends($result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 0);
        
        $result = $this->db->getNewFriends($result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 0);
        
        $result = $this->db->getNewFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 1);
        
        // delete the friends
        $result = $this->db->deleteFriend($result1['person_id'], $result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        // ensure friend was deleted
        $result = $this->db->getFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 2);
        
        $result = $this->db->getFriends($result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 0);
        
        // delete all friends
        $result = $this->db->deleteFriend($result1['person_id'], $result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->deleteFriend($result1['person_id'], $result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        $this->assertEquals($result['message'], "Friends deleted");
        
        // test that no more friends are there
        $result = $this->db->getFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 0);
        
        $result = $this->db->getFriends($result3['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 0);
        
        $result = $this->db->getFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 0);
        
        // get new friends 2 
        $result = $this->db->getNewFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 1);
        
        // refuse a friend request
        $result = $this->db->confirmFriend($result4['person_id'], $result2['person_id'], false);
        $this->assertNotNull($result);
        $this->assertNotNull($result['message']);
        
        // ensure request is gone
        $result = $this->db->getNewFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["newfriends"]));
        $this->assertEquals(count($result["newfriends"]), 0);
        
        // ensure non friend was added
        $result = $this->db->getFriends($result4['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result["friends"]), 0);
    }
    
    function testUpdateGetLocation(){
         // insert user
        $result1 = $this->db->insertUser(hashString('user@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result1);
        $this->assertTrue(is_array($result1));
        $this->assertNotNull($result1['person_id']);
        
        $result2 = $this->db->insertUser(hashString('friend@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result2);
        $this->assertTrue(is_array($result2));
        $this->assertNotNull($result2['person_id']);
        
         // insert friends        
        $result = $this->db->insertFriend($result1['person_id'], $result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        // confirm the Friends
        $result = $this->db->confirmFriend($result2['person_id'], $result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        // insert new locations
        $result = $this->db->updateLocation($result1['person_id'], 50.50, 10.844989648, 26.12, 10, 'an Address', '2012-03-06 17:33:07' );
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->updateLocation($result1['person_id'], 10.50, 10.844989648, 26.12, 10, 'an Address', '2014-03-06 17:33:07');
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->updateLocation($result2['person_id'], 20.50, 10.844989648, 26.12, 10, 'an Address', '2013-03-06 17:33:07' );
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        $result = $this->db->updateLocation($result2['person_id'], 30.50, 10.844989648, 26.12, 10, 'an Address', '2015-03-06 17:33:07');
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertNotNull($result['message']);
        
        // get friend info
        $result = $this->db->getFriends($result1['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result), 1);
        
        $result = $this->db->getFriends($result2['person_id']);
        $this->assertNotNull($result);
        $this->assertTrue(is_array($result));
        $this->assertTrue(is_array($result["friends"]));
        $this->assertEquals(count($result), 1);
    }

    function testInsertGCM(){
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
        
        $result = $this->db->renovate_gcm_id($result['person_id'], 'ID2');
        $this->assertNotNull($result);
    }
    
    /**
     * @expectedException PDOException
     */
    function testInsertGCMNull(){
        $result = $this->db->insertUser(hashString('testUser@mail.com'), 'PHPUnitTest', 'ID', 'ID');
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
        
        $result = $this->db->renovate_gcm_id($result['person_id'], null);
        $this->assertNotNull($result);
        $this->assertNotNull($result['person_id']);
    }
    
}

?>