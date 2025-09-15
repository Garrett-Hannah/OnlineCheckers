<sub>DISCLAIMER: This project was pulled from a in school project, the project prompt was given by the professor.</sub>

<h1>Chackers*</h1>
<sub>Chackers because it's like checkers lesser known cousin!</sub>

Students:
- Garrett Hannah
- Gillian Wood
- Jonathan Newman
- Riley Singfield
- Brandan Stradling

Project Information:
 - This project is a chackers* Implementation In Java
 - Made as a project for csci2020u Ontario Tech University

---

**Cloning and Running the Project**

Clone the repository using:

git clone https://github.com/[your-username]/SoftDevIntFinal.git
cd SoftDevIntFinal

Build and Run
Using Maven from the terminal:

*Start the Server*
mvn compile
mvn exec:java -Dexec.mainClass="main.StartServer"

*Start the Client*
mvn compile
mvn exec:java -Dexec.mainClass="main.StartChatWindow"

Using an IDE:
Open the project as a Maven project.
Wait for Maven to download dependencies.
Run StartServer and then StartChatWindow from your IDE.

By Default, The Client Connects through the localhost hostname, 
but you can manually change that to another hostname, if you know the IP

Hint: ipconfig

Make sure the socket opened on the Server is the same as the one to access on the client. 

Dependencies
- junit-jupiter – Unit testing (v5.13.0-M2)
- mockito-junit-jupiter – Mocking in tests (v5.13.0)
- gson – JSON serialization/deserialization (v2.12.1)

These are declared in the pom.xml and will be downloaded automatically by Maven.

---

**INSTRUCTIONS:**

- Allows for two players, can support many spectators.
- To Start the game, the HOST must start the game (click 'Start Game')
- Players then go back and forth from their turns.

---

**RULES**
Unlike normal, boring checkers, Chackers* is a new and improved version of that old game.

In Chackers*, each player takes a turn. Pieces can move on the diagonal.
Pieces can also jump over other pieces! This removes the other piece from the board.

Jumping over another piece removes said piece from the board. 

Jumping over another piece maintains your turn, Maybe you can chain together multiple jumps? YOU CAN!

Unlike regular checkers, chackers* was seemingly made by an anarchist! So, no kings. Every chack* is equal, but are some more equal than others? You'll have to find out!

In Chackers*, you can also jump over your own pieces! This allows for more strategy when it comes to how you move about the board.
Is it worth it to sacrifice your friend?

---

**Dependencies**
This project uses the following dependencies:
- JUnit5 (for testing models)
- Mockito (for testing some server interaction)
- Gson (for sending messages over the server)

--- 

**Reflection/Original Outline**

This Project originally started as an entire chess game project.
Unfortunately, Chess would be incredibly difficult to implement, as the server must validate many minute details.
This scope was quickly found to be far too broad for the time allotted. So, instead, it was switched to checkers.

Certain parts of the checkers game couldn't be entirely implemented with the project.

This includes the rule of checkers, where if an opponent's piece is able to be taken, it needs to be taken.

However, this project does represent a very close representation to checkers. (although it is slightly different)

One thing that ended up being very helpful was separating the board from the client GUI. This especially came in handy when it became necessary to validate moves on the server side of the game.

More specifically, things like the command system, with specific enum codes for the server to parse, also became very useful. It was easy to implement a Message to be sent from the client and a way to handle it in the Server.

Although it took a while to set up the proper infrastructure, close to the end of the project, I could quickly create only a few functions to implement a lot of functionality.


**Bugs/Unimplemented Features** 

Bug: Reassigning Team Roles results in both players being spectators.
Unimplemented: Proper Board Checking and move recommendations.


**Where To Go Next?**

If this project were to continued to be worked on, here are some things I would pursue:

1. Regular Checkers Rules: The slightly jank version of checkers displayed was because there wasnt enough time to comb through each rule of checkers, and test and make sure each one follows.
2. 3D interface:Initially, There was work with the Opengl Libraries and LWJGL to display the board as an entire 3D scene. This unfortunately would have taken far too much time to properly set up, as it would have required making even more events for handling not just on a 2D grid, but instead on a 3D plane.
3. Chess! Although It would take a lot of work, this project actually does provide a strong setting to be able to make a Checkers game. Lots of code could be reused as well (including the BoardModel/Position/AbstractPiece classes). Even parts of the GUI be resued, like the Board GUI, and chat windows.








**ScreenShots / Videos**



https://github.com/user-attachments/assets/2a12b874-d8ff-46e2-9bc3-8b179fad8f8a

![image](https://github.com/user-attachments/assets/33fe74e8-5f59-40a7-bb63-4a0676af58fa)
Final Board GUI (after video was recorded)


