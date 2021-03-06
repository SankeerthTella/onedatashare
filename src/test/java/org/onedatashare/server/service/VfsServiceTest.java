/**
 ##**************************************************************
 ##
 ## Copyright (C) 2018-2020, OneDataShare Team, 
 ## Department of Computer Science and Engineering,
 ## University at Buffalo, Buffalo, NY, 14260.
 ## 
 ## Licensed under the Apache License, Version 2.0 (the "License"); you
 ## may not use this file except in compliance with the License.  You may
 ## obtain a copy of the License at
 ## 
 ##    http://www.apache.org/licenses/LICENSE-2.0
 ## 
 ## Unless required by applicable law or agreed to in writing, software
 ## distributed under the License is distributed on an "AS IS" BASIS,
 ## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ## See the License for the specific language governing permissions and
 ## limitations under the License.
 ##
 ##**************************************************************
 */


package org.onedatashare.server.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onedatashare.server.model.core.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VFS Service ")
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VfsServiceTest {
    private static final String email = "vfsTestUser@gmail.com", password = "password", firstName = "VFS", lastName = "User", organization = "testers";

    @Autowired
    private static UserService userService;

    @Autowired
    private static JobService jobService;

    @BeforeAll
    // TODO: Initialize with a valid user and credentials
    public static void initialize(){
        assertNotNull(userService);
        assertNotNull(jobService);
        userService.register(email, firstName, lastName, organization, "");
        User user = userService.getUser(email).block();
        System.out.println(user);
    }

    @AfterAll
    // TODO: Delete the user and credentials
    public static void cleanUp(){
    }

    @Test
    @DisplayName("testing list")
    public void listTest(){

    }

    @Test
    @DisplayName("testing mkdir")
    public void mkdirTest(){

    }

    @Test
    @DisplayName("testing delete")
    public void deleteTest(){

    }

    @Test
    @DisplayName("testing download URL")
    public void getDownloadURLTest(){

    }

    @Test
    @DisplayName("testing submit")
    public void submitTest(){

    }

}