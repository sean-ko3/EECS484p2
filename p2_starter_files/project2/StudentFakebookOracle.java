package project2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.ArrayList;

/*
    The StudentFakebookOracle class is derived from the FakebookOracle class and implements
    the abstract query functions that investigate the database provided via the <connection>
    parameter of the constructor to discover specific information.
*/
public final class StudentFakebookOracle extends FakebookOracle {
    // [Constructor]
    // REQUIRES: <connection> is a valid JDBC connection
    public StudentFakebookOracle(Connection connection) {
        oracle = connection;
    }

    @Override
    // Query 0
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the total number of users for which a birth month is listed
    //        (B) Find the birth month in which the most users were born
    //        (C) Find the birth month in which the fewest users (at least one) were born
    //        (D) Find the IDs, first names, and last names of users born in the month
    //            identified in (B)
    //        (E) Find the IDs, first names, and last name of users born in the month
    //            identified in (C)
    //
    // This query is provided to you completed for reference. Below you will find the appropriate
    // mechanisms for opening up a statement, executing a query, walking through results, extracting
    // data, and more things that you will need to do for the remaining nine queries
    public BirthMonthInfo findMonthOfBirthInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            // Step 1
            // ------------
            // * Find the total number of users with birth month info
            // * Find the month in which the most users were born
            // * Find the month in which the fewest (but at least 1) users were born
            ResultSet rst = stmt.executeQuery(
                    "SELECT COUNT(*) AS Birthed, Month_of_Birth " + // select birth months and number of uses with that birth month
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth IS NOT NULL " + // for which a birth month is available
                            "GROUP BY Month_of_Birth " + // group into buckets by birth month
                            "ORDER BY Birthed DESC, Month_of_Birth ASC"); // sort by users born in that month, descending; break ties by birth month

            int mostMonth = 0;
            int leastMonth = 0;
            int total = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    mostMonth = rst.getInt(2); //   it is the month with the most
                }
                if (rst.isLast()) { // if last record
                    leastMonth = rst.getInt(2); //   it is the month with the least
                }
                total += rst.getInt(1); // get the first field's value as an integer
            }
            BirthMonthInfo info = new BirthMonthInfo(total, mostMonth, leastMonth);

            // Step 2
            // ------------
            // * Get the names of users born in the most popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + mostMonth + " " + // born in the most popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addMostPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 3
            // ------------
            // * Get the names of users born in the least popular birth month
            rst = stmt.executeQuery(
                    "SELECT User_ID, First_Name, Last_Name " + // select ID, first name, and last name
                            "FROM " + UsersTable + " " + // from all users
                            "WHERE Month_of_Birth = " + leastMonth + " " + // born in the least popular birth month
                            "ORDER BY User_ID"); // sort smaller IDs first

            while (rst.next()) {
                info.addLeastPopularBirthMonthUser(new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3)));
            }

            // Step 4
            // ------------
            // * Close resources being used
            rst.close();
            stmt.close(); // if you close the statement first, the result set gets closed automatically

            return info;

        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new BirthMonthInfo(-1, -1, -1);
        }
    }

    @Override
    // Query 1
    // -----------------------------------------------------------------------------------
    // GOALS: (A) The first name(s) with the most letters
    //        (B) The first name(s) with the fewest letters
    //        (C) The first name held by the most users
    //        (D) The number of users whose first name is that identified in (C)
    public FirstNameInfo findNameInfo() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {

            ResultSet rst = stmt.executeQuery("SELECT DISTINCT first_name, LENGTH(first_name) AS len " +
                                                "FROM " + UsersTable + " " + 
                                                "ORDER BY len DESC");

            FirstNameInfo info = new FirstNameInfo();

            int longestlength = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    longestlength = rst.getInt(2);
                    info.addLongName(rst.getString(1));
                }
                else{
                    if(rst.getInt(2) == longestlength){
                        info.addLongName(rst.getString(1));
                    }
                }
            }

            rst = stmt.executeQuery("SELECT DISTINCT first_name, LENGTH(first_name) AS len " +
                                                "FROM " + UsersTable + " " + 
                                                "ORDER BY len ASC");

            int shortlength = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    shortlength = rst.getInt(2);
                    info.addShortName(rst.getString(1));
                }
                else{
                    if(rst.getInt(2) == shortlength){
                        info.addShortName(rst.getString(1));
                    }
                }
            }

            rst = stmt.executeQuery("SELECT DISTINCT first_name, COUNT(*) AS count " +
                                                "FROM " + UsersTable + " " + 
                                                "GROUP BY first_name " +
                                                "ORDER BY count DESC"
                                                );
 
            int commonlength = 0;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    commonlength = rst.getInt(2);
                    info.setCommonNameCount(commonlength);
                    info.addCommonName(rst.getString(1));
                }
                else{
                    if(rst.getInt(2) == commonlength){
                        info.addCommonName(rst.getString(1));
                    }
                }
            }
            rst.close();
            stmt.close();
            return info;
            

            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                FirstNameInfo info = new FirstNameInfo();
                info.addLongName("Aristophanes");
                info.addLongName("Michelangelo");
                info.addLongName("Peisistratos");
                info.addShortName("Bob");
                info.addShortName("Sue");
                info.addCommonName("Harold");
                info.addCommonName("Jessica");
                info.setCommonNameCount(42);
                return info;
            */
            //return new FirstNameInfo(); // placeholder for compilation
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new FirstNameInfo();
        }
    }

    @Override
    // Query 2
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users without any friends
    //
    // Be careful! Remember that if two users are friends, the Friends table only contains
    // the one entry (U1, U2) where U1 < U2.
    public FakebookArrayList<UserInfo> lonelyUsers() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {

            ResultSet rst = stmt.executeQuery("SELECT DISTINCT user_id, first_name, last_name " +
                                                "FROM " + UsersTable + " " +
                                                "WHERE user_id NOT IN " + 
                                                "( " + 
                                                "SELECT user1_id AS user_id " +
                                                "FROM " + FriendsTable + " " + 
                                                "UNION " + 
                                                "SELECT user2_id AS user_id " +
                                                "FROM " + FriendsTable + 
                                                ") " + "ORDER BY user_id ASC"
                                                );

            


            while (rst.next()) { // step through result rows/records one by one
                UserInfo info = new UserInfo(rst.getInt(1), rst.getString(2), rst.getString(3));
                results.add(info);
            }
            
            rst.close();
            stmt.close();
            return results;

            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(15, "Abraham", "Lincoln");
                UserInfo u2 = new UserInfo(39, "Margaret", "Thatcher");
                results.add(u1);
                results.add(u2);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 3
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of users who no longer live
    //            in their hometown (i.e. their current city and their hometown are different)
    public FakebookArrayList<UserInfo> liveAwayFromHome() throws SQLException {
        FakebookArrayList<UserInfo> results = new FakebookArrayList<UserInfo>(", ");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {


            ResultSet rst = stmt.executeQuery("SELECT DISTINCT " + UsersTable + ".user_id, " + UsersTable +  ".first_name," + UsersTable + ".last_name" +
                                                " FROM " + UsersTable + ", " + HometownCitiesTable + ", " + CurrentCitiesTable + " " +
                                                "WHERE " + UsersTable + ".user_id = " + HometownCitiesTable + ".user_id AND " 
                                                + UsersTable + ".user_id = " + CurrentCitiesTable + ".user_id AND " + HometownCitiesTable + ".hometown_city_id IS NOT NULL AND "
                                                + CurrentCitiesTable + ".current_city_id IS NOT NULL AND "
                                                + HometownCitiesTable + ".hometown_city_id != " + CurrentCitiesTable + ".current_city_id"  +
                                                " ORDER BY " + UsersTable + ".user_id ASC"
                                                );
            while (rst.next()) { // step through result rows/records one by one
                UserInfo info = new UserInfo(rst.getInt(1), rst.getString(2), rst.getString(3));
                results.add(info);
            }
            
            rst.close();
            stmt.close();
            return results;
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(9, "Meryl", "Streep");
                UserInfo u2 = new UserInfo(104, "Tom", "Hanks");
                results.add(u1);
                results.add(u2);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 4
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, links, and IDs and names of the containing album of the top
    //            <num> photos with the most tagged users
    //        (B) For each photo identified in (A), find the IDs, first names, and last names
    //            of the users therein tagged
    public FakebookArrayList<TaggedPhotoInfo> findPhotosWithMostTags(int num) throws SQLException {
        FakebookArrayList<TaggedPhotoInfo> results = new FakebookArrayList<TaggedPhotoInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*ResultSet rst = stmt.executeUpdate("CREATE VIEW AS (" + photos_tags + "SELECT " +  TagsTable + ".tag_photo_id, COUNT(DISTINCT *) AS count, " +
                                                PhotosTable + ".photo_link," +  AlbumsTable + ".album_id, " + AlbumsTable + ".album_name"  +
                                                "FROM " + TagsTable + ", " + PhotosTable + ", " + AlbumsTable + " " + 
                                                "WHERE " + TagsTable + ".tag_photo_id = " + PhotosTable + ".photo_id AND " 
                                                + PhotosTable + ".album_id = " + AlbumsTable + ".album_id " + 
                                                "GROUP BY " + TagsTable + ".tag_photo_id, " + AlbumsTable + ".album_id, " + PhotosTable + ".photo_link, " + 
                                                AlbumsTable + ".album_name " + 
                                                "ORDER BY count DESC, tag_photo_id ASC" + 
                                                "WHERE ROWNUM < " + num + ")"
                                                );
            
            ResultSet rst2 = stmt.executeQuery( "SELECT " + photos_tags + ".photo_id," + photos_tags+ ".album_id, " + 
                                                photos_tags + ".photo_link, " + photos_tags + ".album_name, " +  
                                                TagsTable + ".tag_subject_id" + UsersTable + ".first_name "+ UsersTable + ".last_name "
                                                " FROM " + photos_tags + " JOIN " + TagsTable " ON "  + TagsTable + ".tag_photo_id =" + photos_tags + ".photo_id "
                                                " JOIN " + UsersTable + " ON  " + TagsTable + ".tag_subject_id = " + UsersTable + ".user_id" + 
                                                "ORDER BY " + UsersTable + ".user_id");
            //go through results and for every row that have the same photo, add the tagged user.
            //if this photo id has not been seen before, make a new PhotoInfo class and add to 
            while (rst2.next()) { // step through result rows/records one by one
                long photo_id = rst2.getlong(1)
                while
                }
            
          
            }
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                PhotoInfo p = new PhotoInfo(80, 5, "www.photolink.net", "Winterfell S1");
                UserInfo u1 = new UserInfo(3901, "Jon", "Snow");
                UserInfo u2 = new UserInfo(3902, "Arya", "Stark");
                UserInfo u3 = new UserInfo(3903, "Sansa", "Stark");
                TaggedPhotoInfo tp = new TaggedPhotoInfo(p);
                tp.addTaggedUser(u1);
                tp.addTaggedUser(u2);
                tp.addTaggedUser(u3);
                results.add(tp);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 5
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, last names, and birth years of each of the two
    //            users in the top <num> pairs of users that meet each of the following
    //            criteria:
    //              (i) same gender
    //              (ii) tagged in at least one common photo
    //              (iii) difference in birth years is no more than <yearDiff>
    //              (iv) not friends
    //        (B) For each pair identified in (A), find the IDs, links, and IDs and names of
    //            the containing album of each photo in which they are tagged together
    public FakebookArrayList<MatchPair> matchMaker(int num, int yearDiff) throws SQLException {
        FakebookArrayList<MatchPair> results = new FakebookArrayList<MatchPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(93103, "Romeo", "Montague");
                UserInfo u2 = new UserInfo(93113, "Juliet", "Capulet");
                MatchPair mp = new MatchPair(u1, 1597, u2, 1597);
                PhotoInfo p = new PhotoInfo(167, 309, "www.photolink.net", "Tragedy");
                mp.addSharedPhoto(p);
                results.add(mp);
            */
        ResultSet rst  = stmt.executeQuery("SELECT users.user1id, users.user2id, users.user1firstname, users.user1lastname, users.user1yob, users.user2firstname, users.user2lastname, users.user2yob, COUNT(*) AS count
        FROM (SELECT u1.user_id AS user1id,  u1.first_name AS user1firstname, u1.last_name  AS user1lastname,  u1.year_of_birth AS user1yob,
        u2.user_id AS user2id,  u2.first_name AS user2firstname, u2.last_name AS user2lastname,  u2.year_of_birth AS user2yob
        FROM " + UsersTable + " u1
        JOIN " + UsersTable + " u2 WHERE u1.user_id < u2.user_id AND 
        u1.gender == u2.gender AND ABS(u1.year_of_birth - u2.year_of_birth) < yearDiff
        AND NOT EXISTS (
            SELECT 1 
            FROM " + FriendsTable + " f
            WHERE (f.user1_id = u1.user_id AND f.user2_id = u2.user_id) OR
            (f.user1_id = u2.user_id AND f.user2_id = u1.user_id) 
        )
        AND EXISTS (
            SELECT 1 
            FROM " + TagsTable + " t1, " + TagsTable + " t2
            WHERE u1.user_id = t1.tag_subject_id AND u2.user_id = t2.tag_subject_id AND
            t1.photo_id = t2.photo_id
        )) users
        JOIN " + TagsTable + " t1 ON users.user_id = t1.tag_subject_id
        JOIN " + TagsTable + " t2 ON users.user_id = t2.tag_subject_id AND t1.photo_id = t2.photo_id
        GROUP BY users.user1id, users.user2id, users.user1firstname, users.user1lastname, users.user1yob, users.user2firstname, users.user2lastname, users.user2yob 
        ORDER BY users.count DESC, users.user1id ASC, users.user2id ASC ");

        int counter = 0;
        while (rst.next() && counter < num) { // step through result rows/records one by one
            UserInfo u1 = new UserInfo(rst.getLong(1), rst.getString(3), rst.getString(4));
            UserInfo u2 = new UserInfo(rst.getLong(2), rst.getString(6), rst.getString(7));
            MatchPair m = new MatchPair(u1, rst.getlong(5), rst.getlong(8));
            ResultSet rst2 = stmt.executeQuery(
                "SELECT p.photo_id, p.photo_link, p.album_id, a.album_name
                FROM " + TagsTable + " t1, " + TagsTable + " t2, " + PhotosTable + " p, " + Albums + " a
                WHERE " + rst.getLong(1) + " = t1.tag_subject_id AND " + rst.getLong(2) + " = t2.tag_subject_id AND t1.photo_id = t2.photo_id AND p.photo_id = t1.photo_id AND p.album_id = a.album_id
                ORDER BY p.photo_id ASC"
            );
            while(rst2.next()){
                PhotoInfo p = new PhotoInfo(rst2.getlong(1), rst2.getlong(3), rst2.getString(2), rst2.getString(4));
                m.addSharedPhoto(p);
            }
            rst2.close();
            results.add(m);
            counter += 1;
        }

        rst.close();
        stmt.close();


        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 6
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the IDs, first names, and last names of each of the two users in
    //            the top <num> pairs of users who are not friends but have a lot of
    //            common friends
    //        (B) For each pair identified in (A), find the IDs, first names, and last names
    //            of all the two users' common friends
    public FakebookArrayList<UsersPair> suggestFriends(int num) throws SQLException {
        FakebookArrayList<UsersPair> results = new FakebookArrayList<UsersPair>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            SELECT
            FROM FriendsTable f1, FriendsTable f2
            WHERE f1.user
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(16, "The", "Hacker");
                UserInfo u2 = new UserInfo(80, "Dr.", "Marbles");
                UserInfo u3 = new UserInfo(192, "Digit", "Le Boid");
                UsersPair up = new UsersPair(u1, u2);
                up.addSharedFriend(u3);
                results.add(up);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    @Override
    // Query 7
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the name of the state or states in which the most events are held
    //        (B) Find the number of events held in the states identified in (A)
    public EventStateInfo findEventStates() throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {

            ResultSet rst = stmt.executeQuery("SELECT " + CitiesTable + ".state_name, " + "COUNT(*) as count" +
                                                " FROM " + EventsTable + ", " + CitiesTable + " " +
                                                "WHERE " + EventsTable + ".event_city_id = " + CitiesTable + ".city_id" + 
                                                " GROUP BY " + CitiesTable + ".state_name " + 
                                                "ORDER BY count DESC, " + CitiesTable + ".state_name ASC");
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                EventStateInfo info = new EventStateInfo(50);
                info.addState("Kentucky");
                info.addState("Hawaii");
                info.addState("New Hampshire");
                return info;
            */
            rst.next();
            EventStateInfo info = new EventStateInfo(rst.getInt(2));
            info.addState(rst.getString(1));

            int longestlength = rst.getInt(2);
            while (rst.next()) { // step through result rows/records one by one
                    if(rst.getInt(2) == longestlength){
                        info.addState(rst.getString(1));
                    }
                }
            
            rst.close();
            stmt.close();
            return info;

            //return new EventStateInfo(-1); // placeholder for compilation
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new EventStateInfo(-1);
        }
    }

    @Override
    // Query 8
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find the ID, first name, and last name of the oldest friend of the user
    //            with User ID <userID>
    //        (B) Find the ID, first name, and last name of the youngest friend of the user
    //            with User ID <userID>
    public AgeInfo findAgeInfo(long userID) throws SQLException {
        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            ResultSet rst = stmt.executeQuery("SELECT " + UsersTable + ".user_id, " + UsersTable + ".first_name, " + UsersTable + ".last_name " + 
                                                "FROM " + UsersTable + " " +
                                                "JOIN (" + 
                                                "SELECT user2_id AS user_id " +
                                                "FROM " + FriendsTable + " " +
                                                "WHERE user1_id = " + userID +
                                                " UNION " +
                                                "SELECT user1_id AS user_id " +
                                                "FROM " + FriendsTable +
                                                " WHERE user2_id = " + userID + ") friends ON friends.user_id = " + UsersTable + ".user_id " + 
                                                "ORDER BY " + UsersTable + ".year_of_birth ASC, " + UsersTable + ".month_of_birth ASC, " + UsersTable + ".day_of_birth ASC, " + UsersTable + ".user_id DESC"
                                                );
            UserInfo old = null;
            UserInfo newer = null;
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    old = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                }
            
            }
            rst = stmt.executeQuery("SELECT " + UsersTable + ".user_id, " + UsersTable + ".first_name, " + UsersTable + ".last_name " + 
                                                "FROM " + UsersTable + " " +
                                                "JOIN (" + 
                                                "SELECT user2_id AS user_id " +
                                                "FROM " + FriendsTable + " " +
                                                "WHERE user1_id = " + userID +
                                                " UNION " +
                                                "SELECT user1_id AS user_id " +
                                                "FROM " + FriendsTable +
                                                " WHERE user2_id = " + userID + ") friends ON friends.user_id = " + UsersTable + ".user_id " + 
                                                "ORDER BY " + UsersTable + ".year_of_birth DESC, " + UsersTable + ".month_of_birth DESC, " + UsersTable + ".day_of_birth DESC, " + UsersTable + ".user_id DESC"
                                                );
            while (rst.next()) { // step through result rows/records one by one
                if (rst.isFirst()) { // if first record
                    newer = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                }
            rst.close();
            stmt.close();
            return new AgeInfo(old, newer);
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo old = new UserInfo(12000000, "Galileo", "Galilei");
                UserInfo young = new UserInfo(80000000, "Neil", "deGrasse Tyson");
                return new AgeInfo(old, young);
            */
            //return new AgeInfo(new UserInfo(-1, "UNWRITTEN", "UNWRITTEN"), new UserInfo(-1, "UNWRITTEN", "UNWRITTEN")); // placeholder for compilation
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return new AgeInfo(new UserInfo(-1, "ERROR", "ERROR"), new UserInfo(-1, "ERROR", "ERROR"));
        }
    }

    @Override
    // Query 9
    // -----------------------------------------------------------------------------------
    // GOALS: (A) Find all pairs of users that meet each of the following criteria
    //              (i) same last name
    //              (ii) same hometown
    //              (iii) are friends
    //              (iv) less than 10 birth years apart
    public FakebookArrayList<SiblingInfo> findPotentialSiblings() throws SQLException {
        FakebookArrayList<SiblingInfo> results = new FakebookArrayList<SiblingInfo>("\n");

        try (Statement stmt = oracle.createStatement(FakebookOracleConstants.AllScroll,
                FakebookOracleConstants.ReadOnly)) {
            ResultSet rst = stmt.executeQuery(
                "SELECT U1.user_id, U1.first_name, U1.last_name, U2.user_id, U2.first_name, U2.last_name " +
                "FROM " + FriendsTable + " " +
                "JOIN " + UsersTable + " U1 ON " + FriendsTable + ".user1_id = U1.user_id " +
                "JOIN " + UsersTable + " U2 ON " + FriendsTable + ".user2_id = U2.user_id " +
                "JOIN " + HometownCitiesTable + " H1 ON U1.user_id = H1.user_id " +
                "JOIN " + HometownCitiesTable + " H2 ON U2.user_id = H2.user_id " +
                "WHERE U1.last_name = U2.last_name " +
                "AND H1.hometown_city_id = H2.hometown_city_id " +
                "AND ABS(U1.year_of_birth - U2.year_of_birth) < 10" + 
                " ORDER BY " + "U1.user_id ASC,  U2.user_id ASC" 
            );

            while (rst.next()) { // step through result rows/records one by one
                UserInfo u1 = new UserInfo(rst.getLong(1), rst.getString(2), rst.getString(3));
                UserInfo u2 = new UserInfo(rst.getLong(4), rst.getString(5), rst.getString(6));
                SiblingInfo si = new SiblingInfo(u1, u2);
                results.add(si);
            }
            rst.close();
            stmt.close();
            /*
                EXAMPLE DATA STRUCTURE USAGE
                ============================================
                UserInfo u1 = new UserInfo(81023, "Kim", "Kardashian");
                UserInfo u2 = new UserInfo(17231, "Kourtney", "Kardashian");
                SiblingInfo si = new SiblingInfo(u1, u2);
                results.add(si);
            */
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }

    // Member Variables
    private Connection oracle;
    private final String UsersTable = FakebookOracleConstants.UsersTable;
    private final String CitiesTable = FakebookOracleConstants.CitiesTable;
    private final String FriendsTable = FakebookOracleConstants.FriendsTable;
    private final String CurrentCitiesTable = FakebookOracleConstants.CurrentCitiesTable;
    private final String HometownCitiesTable = FakebookOracleConstants.HometownCitiesTable;
    private final String ProgramsTable = FakebookOracleConstants.ProgramsTable;
    private final String EducationTable = FakebookOracleConstants.EducationTable;
    private final String EventsTable = FakebookOracleConstants.EventsTable;
    private final String AlbumsTable = FakebookOracleConstants.AlbumsTable;
    private final String PhotosTable = FakebookOracleConstants.PhotosTable;
    private final String TagsTable = FakebookOracleConstants.TagsTable;
}
