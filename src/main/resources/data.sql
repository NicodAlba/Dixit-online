-- One admin user, named admin1 with passwor 4dm1n and authority admin
INSERT INTO authorities(id, authority) VALUES (1, 'ADMIN');
INSERT INTO authorities(id, authority) VALUES (2, 'PLAYER');
INSERT INTO appusers(id,username,password,authority_id) VALUES (1,'admin1','$2a$10$nMmTWAhPTqXqLDJTag3prumFrAJpsYtroxf0ojesFYq0k4PmcbWUS',1);

-- Ten player users, named player1 with passwor 0wn3r
INSERT INTO appusers(id,username,password,authority_id) VALUES (4,'player1','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (5,'player2','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (6,'player3','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (7,'player4','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (8,'player5','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (9,'player6','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (10,'player7','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (11,'player8','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (12,'player9','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (13,'player10','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (14,'xsk0385','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (15, 'xcn8537','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e', 2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (16, 'nyp0448','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e', 2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (17,'pns5576','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);
INSERT INTO appusers(id,username,password,authority_id) VALUES (18,'gfq0792','$2a$10$DaS6KIEfF5CRTFrxIoGc7emY3BpZZ0.fVjwA3NiJ.BjpGNmocaS3e',2);

INSERT INTO images (id, url) VALUES (0, '/Cards/0.png');
INSERT INTO images (id, url) VALUES (1, '/Cards/1.png');
INSERT INTO images (id, url) VALUES (2, '/Cards/2.png');
INSERT INTO images (id, url) VALUES (3, '/Cards/3.png');
INSERT INTO images (id, url) VALUES (4, '/Cards/4.png');
INSERT INTO images (id, url) VALUES (5, '/Cards/5.png');
INSERT INTO images (id, url) VALUES (6, '/Cards/6.png');
INSERT INTO images (id, url) VALUES (7, '/Cards/7.png');
INSERT INTO images (id, url) VALUES (8, '/Cards/8.png');
INSERT INTO images (id, url) VALUES (9, '/Cards/9.png');
INSERT INTO images (id, url) VALUES (10, '/Cards/10.png');
INSERT INTO images (id, url) VALUES (11, '/Cards/11.png');
INSERT INTO images (id, url) VALUES (12, '/Cards/12.png');
INSERT INTO images (id, url) VALUES (13, '/Cards/13.png');
INSERT INTO images (id, url) VALUES (14, '/Cards/14.png');
INSERT INTO images (id, url) VALUES (15, '/Cards/15.png');
INSERT INTO images (id, url) VALUES (16, '/Cards/16.png');
INSERT INTO images (id, url) VALUES (17, '/Cards/17.png');
INSERT INTO images (id, url) VALUES (18, '/Cards/18.png');
INSERT INTO images (id, url) VALUES (19, '/Cards/19.png');
INSERT INTO images (id, url) VALUES (20, '/Cards/20.png');
INSERT INTO images (id, url) VALUES (21, '/Cards/21.png');
INSERT INTO images (id, url) VALUES (22, '/Cards/22.png');
INSERT INTO images (id, url) VALUES (23, '/Cards/23.png');
INSERT INTO images (id, url) VALUES (24, '/Cards/24.png');
INSERT INTO images (id, url) VALUES (25, '/Cards/25.png');
INSERT INTO images (id, url) VALUES (26, '/Cards/26.png');
INSERT INTO images (id, url) VALUES (27, '/Cards/27.png');
INSERT INTO images (id, url) VALUES (28, '/Cards/28.png');
INSERT INTO images (id, url) VALUES (29, '/Cards/29.png');
INSERT INTO images (id, url) VALUES (30, '/Cards/30.png');
INSERT INTO images (id, url) VALUES (31, '/Cards/31.png');
INSERT INTO images (id, url) VALUES (32, '/Cards/32.png');
INSERT INTO images (id, url) VALUES (33, '/Cards/33.png');
INSERT INTO images (id, url) VALUES (34, '/Cards/34.png');
INSERT INTO images (id, url) VALUES (35, '/Cards/35.png');

INSERT INTO badge_images (id, url, type) VALUES (0, '/Achievements/WinAchievements/Win1.png', 'WIN'),
                                                (1, '/Achievements/WinAchievements/Win5.png', 'WIN'),
                                                (2, '/Achievements/WinAchievements/Win10.png', 'WIN'),
                                                (3, '/Achievements/WinAchievements/Win50.png', 'WIN');

INSERT INTO badge_images (id, url, type) VALUES (4, '/Achievements/PlayAchievements/Play1.png', 'PLAY'),
                                                (5, '/Achievements/PlayAchievements/Play5.png', 'PLAY'),
                                                (6, '/Achievements/PlayAchievements/Play10.png', 'PLAY'),
                                                (7, '/Achievements/PlayAchievements/Play50.png', 'PLAY');

INSERT INTO badge_images (id, url, type) VALUES (8, '/Achievements/TrickAchievements/Trick5.png', 'TRICK'),
                                                (9, '/Achievements/TrickAchievements/Trick20.png', 'TRICK'),
                                                (10, '/Achievements/TrickAchievements/Trick50.png', 'TRICK'),
                                                (11, '/Achievements/TrickAchievements/Trick100.png', 'TRICK');

