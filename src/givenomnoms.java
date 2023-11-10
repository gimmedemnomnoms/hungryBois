import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class givenomnoms {
    static final int NUM_CUSTOMERS = 40;
    public static int customerCounter = NUM_CUSTOMERS;
    public static final Object printlock = new Object();
    // Semaphores for only one customer to be allowed through a door at a time
    public static Semaphore doorOneSemaphore = new Semaphore(1);
    public static Semaphore doorTwoSemaphore = new Semaphore(1);
    // Semaphores for each table to limit only 4 customers seated at a time
    public static Semaphore tableOneSemaphore = new Semaphore(4);
    public static Semaphore tableTwoSemaphore = new Semaphore(4);
    public static Semaphore tableThreeSemaphore = new Semaphore(4);

    // Semaphore for only one waiter to be allowed in the kitchen at a time
    public static Semaphore kitchenSemaphore = new Semaphore(1);

    // Semaphore to call each waiter, so that only one customer can interact at a time
    public static Semaphore waiterOneSemaphore = new Semaphore(1);
    public static Semaphore waiterTwoSemaphore = new Semaphore(1);
    public static Semaphore waiterThreeSemaphore = new Semaphore(1);

    // Semaphore to pay bill at cashier
    public static Semaphore cashierSemaphore = new Semaphore(1);

    // counters for testing purposes, ensures only 4 customer per table at a time
    public static int aCounter, bCounter, cCounter;
    public static createdThread [] waiter = new createdThread[3];
    public static createdThread [] customer = new createdThread[NUM_CUSTOMERS];

    public static void main(String[] args){
        //create and start waiter threads
        waiter[0] = new createdThread('A');
        waiter[0].start();
        waiter[1] = new createdThread('B');
        waiter[1].start();
        waiter[2] = new createdThread('C');
        waiter[2].start();

        //create and start customer threads
        for (int i = 0; i < NUM_CUSTOMERS; i++){
            customer[i] = new createdThread(i+1);
            customer[i].start();
        }
    }
    public static class createdThread extends Thread{
        public String waiterToString() {
            return "Waiter " + waiterId;
        }

        public String customerToString() {
            return "Customer " + customer_id;
        }

        boolean isCustomer, isWaiter;  // used to differentiate between customer and waiter threads
        int customer_id;
        char waiterId;
        char [] tableChoice = new char[2]; //holds customer's first choice and backup if they have one
        char inLineFor;
        boolean has_backup;
        public Queue<createdThread> tableALine = new LinkedList<>();
        public Queue<createdThread> tableBLine = new LinkedList<>();
        public Queue<createdThread> tableCLine = new LinkedList<>();
        public Random randomTime = new Random();


        public createdThread(int customerId){ //constructor for customer threads
            this.customer_id = customerId;
            isCustomer = true;
            this.isWaiter = false;
            // determine if the customer will have a backup food choice
            Random rand = new Random();
            int decide = rand.nextInt(2);
            if (decide == 1) {
                has_backup = true;
            } else if (decide == 0) {
                has_backup = false;
            }
            chooseFoodChoice();

        }
        public createdThread(char waiterId){ //constructor for waiter threads
            this.waiterId = waiterId;
            isWaiter = true;
            isCustomer = false;
        }
        public void run(){
            if (isWaiter) {
                safePrint(waiterToString() + " is running");
            }

            if (isCustomer) {
                try {
                    useDoor("enters");
                    determineLine();
                    waitToSit();
                    orderFood();
                    Thread.sleep(randomTime.nextInt(800) + 200); //time for customer to eat their food
                    finishAndPay();
                    useDoor("exits");
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } if (customerCounter == 0){
                try {
                    waitersLeave();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        public void chooseFoodChoice(){ //randomly selects a customer's food choice
            Random rand = new Random();
            int primary = rand.nextInt(3);
            if(primary == 0){
                this.tableChoice[0] = 'A';
            }
            else if (primary == 1){
                this.tableChoice[0] = 'B';
            }
            else {
                this.tableChoice[0] = 'C';
            }
            if(has_backup){ //select backup choice if applicable
                chooseBackup(this.tableChoice[0]);
            }
        }

        public void chooseBackup(char primary){
            Random rand = new Random();
            int backup_opt = rand.nextInt(2);
            //randomly choose a customer's backup choice depending on what their first choice is
            if (primary == 'A'){ //if primary is seafood, backup will be steak or pasta
                if (backup_opt == 0) {
                    this.tableChoice[1] = 'B';
                } else if (backup_opt == 1) {
                    this.tableChoice[1] = 'C';
                } else {
                    safePrint("Error: How did you get here?");
                }
            }
            else if (primary == 'B'){ //if primary is steak, backup will be seafood or pasta
                if (backup_opt == 0) {
                    this.tableChoice[1] = 'A';
                } else if (backup_opt == 1) {
                    this.tableChoice[1] = 'C';
                } else {
                    safePrint("Error: How did you get here?");
                }
            }
            else{ //if primary is pasta, backup will be steak or seafood
                if (backup_opt == 0) {
                    this.tableChoice[1] = 'B';
                } else if (backup_opt == 1) {
                    this.tableChoice[1] = 'A';
                } else {
                    safePrint("Error: How did you get here?");
                }
            }
        }

        public void determineLine() throws InterruptedException {
            char primary = tableChoice[0];
            if (!has_backup){
                joinTheLine(primary);
            }
            else { //if customer has backup choice, compare line lengths
                char backup = tableChoice[1];
                int primaryLineLength = checkLineLength(primary);
                if (primaryLineLength < 7){ //customer will go with their first choice if the line is less than 7 people
                    //safePrint("The line for table " + primary + " is not too long, so customer " + customer_id + " will wait");
                    joinTheLine(primary);
                }
                else { //if the line for their first choice is long, check and compare the length of their second choice
                    int backupLineLength = checkLineLength(backup);
                    //safePrint("The line for table " + primary + " is long. Customer " + customer_id + " will check the line for table " + backup);
                    if (backupLineLength < 7){
                        //safePrint("The line for  " + customerToString() + "'s backup choice is shorter.");
                        joinTheLine(backup);
                    }
                    else { //if both lines are long, the customer will go with their first choice
                       // safePrint("The line for Customer " + customer_id + "'s backup choice is also long. They will wait for table " + primary);
                        joinTheLine(primary);
                    }
                }

            }
        }

        public int checkLineLength(char choice){
            if (choice == 'A'){
                return tableALine.size();
            }
            else if (choice == 'B'){
                return tableBLine.size();
            }
            else if (choice == 'C'){
                return tableCLine.size();
            }
            else{
                safePrint("Go away");
                return 1000;
            }
        }
        public void joinTheLine(char option) throws InterruptedException { //adds customer to chosen line
            //getInLineSemaphore.acquire();
            if(option== 'A'){
                tableALine.add(this);
                safePrint(customerToString() + " is in line for Seafood");
            }
            else if (option == 'B'){
                tableBLine.add(this);
                safePrint(customerToString() + " is in line for Steak");
            }
            else if (option == 'C'){
                tableCLine.add(this);
                safePrint(customerToString() + " is in line for Pasta");

            }
            else {
                safePrint("Halt!");
            }
            inLineFor = option;
            //getInLineSemaphore.release();
        }
        public void waitToSit() throws InterruptedException {
            //ensures only one customer leaves the line and is seated at a time
            //beSeatedSemaphore.acquire();
            try { //customer waits for one of four spots at their desired table
                if (inLineFor == 'A'){
                    tableOneSemaphore.acquire();
                    aCounter++;
                    safePrint(customerToString() + " is sitting at table A\tTable A count: " + aCounter);
                }
                else if (inLineFor == 'B'){
                    tableTwoSemaphore.acquire();
                    bCounter++;
                    safePrint(customerToString() + " is sitting at table B\tTable B count: " + bCounter);
                }
                else if (inLineFor == 'C'){
                    tableThreeSemaphore.acquire();
                    cCounter++;
                    safePrint(customerToString() + " is sitting at table C\t table C count: " + cCounter);
                }
                else {
                    safePrint("Get outta here");
                }
                //beSeatedSemaphore.release();

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        public void orderFood() throws InterruptedException {
            char table = inLineFor;
            if (table == 'A'){
                waiterOneSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes customer " + customer_id + "'s order");
                takeOrder(customer_id, table);
                waiterOneSemaphore.release(); //releases waiter to serve other customers
            }
            else if (table == 'B'){
                waiterTwoSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes customer " + customer_id + "'s order");
                takeOrder(customer_id, table);
                waiterTwoSemaphore.release();
            }
            else if (table == 'C'){
                waiterThreeSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes customer " + customer_id + "'s order");
                takeOrder(customer_id, table);
                waiterThreeSemaphore.release();
            }

        }
        public void takeOrder(int customer_id, char waiterId) throws InterruptedException {
                this.customer_id = customer_id;
                kitchenSemaphore.acquire(); //acquires semaphore to be allowed in the kitchen
                //safePrint("Waiter "+ waiterId + " is in the kitchen to drop of Customer " + customer_id + "'s order");
                Thread.sleep(randomTime.nextInt(400) + 100); //time spent in the kitchen delivering the order
                //safePrint("Waiter " + waiterId + " has left the kitchen");
                kitchenSemaphore.release();
                Thread.sleep(randomTime.nextInt(700) + 300); //time spent waiting outside the kitchen for the order to be ready
                kitchenSemaphore.acquire();
                //safePrint("Waiter " + waiterId + " enters the kitchen to pick up customer " + customer_id + "'s order");
                Thread.sleep(randomTime.nextInt(400) + 100); //time spent in kitchen retrieving the order
                //safePrint("Waiter " + waiterId + " leaves the kitchen with customer " + customer_id + "'s order");
                kitchenSemaphore.release();
                safePrint("Waiter " + waiterId + " brings customer " + customer_id + " their food");

        }
        public void finishAndPay() throws InterruptedException {
            safePrint(customerToString() + " has finished eating. They will now leave the table and go to pay");
            //release the table semaphore to allow a new customer to be seated
            if (inLineFor == 'A'){
                aCounter--;
                tableOneSemaphore.release();

            }
            else if (inLineFor == 'B'){
                bCounter--;
                tableTwoSemaphore.release();

            }
            else if (inLineFor == 'C'){
                cCounter--;
                tableThreeSemaphore.release();

            }
            else {
                safePrint("I don't think you should be here");
            }
            cashierSemaphore.acquire(); //only one customer may pay at a time
            //safePrint(customerToString() + " is now at the counter paying");
            cashierSemaphore.release();
            //safePrint(customerToString() + " has finished paying and will now leave the restaurant");
        }

        public void useDoor(String direction) throws InterruptedException {
            Random door = new Random();
            int chosen_door = door.nextInt(2); //randomly choose between 2 doors
            if (chosen_door == 0) {
                doorOneSemaphore.acquire();
                safePrint(customerToString() + " " + direction + " through door 1");
                doorOneSemaphore.release();
            } else {
                doorTwoSemaphore.acquire();
                safePrint(customerToString() + " " + direction + " through door 2");
                doorTwoSemaphore.release();
            }
            if (direction == "exits"){ //if a customer exits, decrement the customer counter to control when waiters leave
                customerCounter--;
                //safePrint(customerCounter + " customers are remaining");
            }
        }
        public void waitersLeave() throws InterruptedException {
            Random door = new Random();
            for (int i = 0; i < waiter.length; i++) {
                int chosen_door = door.nextInt(2);
                if (chosen_door == 0) {
                    doorOneSemaphore.acquire();
                    safePrint("Waiter " + waiter[i].waiterId + " leaves through door 1");
                    doorOneSemaphore.release();
                } else {
                    doorTwoSemaphore.acquire();
                    safePrint("Waiter " + waiter[i].waiterId + " leaves through door 2");
                    doorTwoSemaphore.release();
                }
            }
        }

        public static void safePrint(String line){
            synchronized (printlock){
                System.out.println(line);
            }
        }
    }
}
