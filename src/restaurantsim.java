import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class restaurantsim {
    static final int NUM_CUSTOMERS = 40;
    public static int customerCounter = NUM_CUSTOMERS;
    public static final Object printlock = new Object();
    //Queues to hold how many customers are in each line
    public static Queue<createdThread> tableALine = new LinkedList<>();
    public static Queue<createdThread> tableBLine = new LinkedList<>();
    public static Queue<createdThread> tableCLine = new LinkedList<>();
    public static Semaphore doorSemaphore = new Semaphore(2); //allows 2 to simulate 2 doors each allowing 1 person
    public static Semaphore lineSemaphore = new Semaphore(1);

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
        public String customerToString() {
            return "Customer " + customer_id;
        }

        boolean isCustomer, isWaiter;  // used to differentiate between customer and waiter threads
        int customer_id;
        char waiterId;
        char [] tableChoice = new char[2]; //holds customer's first choice and backup if they have one
        char inLineFor; //holds which line the customer chooses
        boolean has_backup;
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
            }
            else {
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
            if (isWaiter) { //run waiter threads
                safePrint("Waiter " + waiterId + " is running");
            }
            if (isCustomer) { //run customer threads
                try {
                    enterRestaurant();
                    determineLine();
                    waitToSit();
                    orderFood();
                    customerEats();
                    finishAndPay();
                    exitRestaurant();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if (customerCounter == 0){ //checks if the customer count is 0 so waiters will leave
                try {
                    waitersLeave();
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        public void chooseFoodChoice(){ //randomly selects a customer's food choice
            Random rand = new Random();
            int primary = rand.nextInt(3);
            if (primary == 0){
                this.tableChoice[0] = 'A';
                safePrint(customerToString() + " wants to eat seafood");
            }
            else if (primary == 1){
                this.tableChoice[0] = 'B';
                safePrint(customerToString() + " wants to eat steak");
            }
            else {
                this.tableChoice[0] = 'C';
                safePrint(customerToString() + " wants to eat pasta");
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
                }
                else {
                    this.tableChoice[1] = 'C';
                }
            }
            else if (primary == 'B'){ //if primary is steak, backup will be seafood or pasta
                if (backup_opt == 0) {
                    this.tableChoice[1] = 'A';
                }
                else {
                    this.tableChoice[1] = 'C';
                }
            }
            else { //if primary is pasta, backup will be steak or seafood
                if (backup_opt == 0) {
                    this.tableChoice[1] = 'B';
                }
                else {
                    this.tableChoice[1] = 'A';
                }
            }
        }

        public void determineLine() throws InterruptedException {
            lineSemaphore.acquire();
            char primary = tableChoice[0];
            if (!has_backup){
                safePrint(customerToString() + " stands in line for table " + primary);
                joinTheLine(primary);
            }
            else { //if customer has backup choice, compare line lengths
                char backup = tableChoice[1];
                int primaryLineLength = checkLineLength(primary);
                if (primaryLineLength < 7){ //customer will go with their first choice if the line is less than 7 people
                    safePrint(customerToString() + "'s first choice does not have a long line\n" + customerToString() + " stands in line for table " + primary);
                    joinTheLine(primary);
                }
                else { //if the line for their first choice is long, check and compare the length of their second choice
                    int backupLineLength = checkLineLength(backup);
                   safePrint(customerToString() + "'s first choice has a long line");
                    if (backupLineLength < 7){
                        safePrint(customerToString() + "'s backup choice does not have a long line\n" + customerToString() +  " stands in line for table " + backup);
                        joinTheLine(backup);
                    }
                    else { //if both lines are long, the customer will go with their first choice
                       safePrint(customerToString() + "'s backup choice has a long line\n" + customerToString() + " stands in line for table " + primary);
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
                safePrint("Error: checkLineLength");
                return 1000;
            }
        }
        public void joinTheLine(char option){ //adds customer to chosen line
            if(option== 'A'){
                tableALine.add(this);
            }
            else if (option == 'B'){
                tableBLine.add(this);
            }
            else if (option == 'C'){
                tableCLine.add(this);
            }
            else {
                safePrint("Error: joinTheLine");
            }
            inLineFor = option;
            lineSemaphore.release();
        }
        public void waitToSit() throws InterruptedException {
            try { //customer waits for one of four spots at their desired table
                if (inLineFor == 'A'){
                    tableOneSemaphore.acquire();
                    tableALine.remove(this);
                    safePrint(customerToString() + " is sitting at table A");
                }
                else if (inLineFor == 'B'){
                    tableTwoSemaphore.acquire();
                    tableBLine.remove(this);
                    safePrint(customerToString() + " is sitting at table B");
                }
                else if (inLineFor == 'C'){
                    tableThreeSemaphore.acquire();
                    tableCLine.remove(this);
                    safePrint(customerToString() + " is sitting at table C");
                }
                else {
                    safePrint("Error: waitToSit");
                }
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        public void orderFood() throws InterruptedException {
            char table = inLineFor;
            safePrint(customerToString() + " calls waiter " + table);
            if (table == 'A'){
                waiterOneSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes " + customerToString() + "'s order");
                takeOrder(customer_id, table);
                waiterOneSemaphore.release(); //releases waiter to serve other customers
            }
            else if (table == 'B'){
                waiterTwoSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes " + customerToString() + "'s order");
                takeOrder(customer_id, table);
                waiterTwoSemaphore.release();
            }
            else if (table == 'C'){
                waiterThreeSemaphore.acquire(); //ensures waiter only serves one customer at a time
                safePrint("Waiter " + table + " takes " + customerToString() + "'s order");
                takeOrder(customer_id, table);
                waiterThreeSemaphore.release();
            }
        }
        public void takeOrder(int customer_id, char waiterId) throws InterruptedException {
                this.customer_id = customer_id;
                safePrint("Waiter " + waiterId + " goes to the kitchen to deliver " + customerToString() + "'s order");
                kitchenSemaphore.acquire(); //acquires semaphore to be allowed in the kitchen
                safePrint("Waiter "+ waiterId + " enters the kitchen to deliver " + customerToString() + "'s order");
                Thread.sleep(randomTime.nextInt(400) + 100); //time spent in the kitchen delivering the order
                safePrint("Waiter " + waiterId + " exits the kitchen and waits");
                kitchenSemaphore.release();
                Thread.sleep(randomTime.nextInt(700) + 300); //time spent waiting outside the kitchen for the order to be ready
                kitchenSemaphore.acquire();
                safePrint("Waiter " + waiterId + " enters the kitchen to pick up " + customerToString() + "'s order");
                Thread.sleep(randomTime.nextInt(400) + 100); //time spent in kitchen retrieving the order
                safePrint("Waiter " + waiterId + " exits the kitchen");
                kitchenSemaphore.release();
                safePrint("Waiter " + waiterId + " brings food to " + customerToString());

        }
        public void customerEats() throws InterruptedException {
            safePrint(customerToString() + " eats");
            Thread.sleep(randomTime.nextInt(800) + 200); //time for customer to eat their food
            safePrint(customerToString() + " is done eating");
        }
        public void finishAndPay() throws InterruptedException {
            safePrint(customerToString() + " leaves the table and goes to pay");
            //release the table semaphore to allow a new customer to be seated
            if (inLineFor == 'A'){
                tableOneSemaphore.release();
            }
            else if (inLineFor == 'B'){
                tableTwoSemaphore.release();
            }
            else if (inLineFor == 'C'){
                tableThreeSemaphore.release();
            }
            else {
                safePrint("Error: finishAndPay");
            }
            cashierSemaphore.acquire(); //only one customer may pay at a time
            safePrint(customerToString() + " pays");
            cashierSemaphore.release();
        }
        public void enterRestaurant() throws InterruptedException { //controls two doors, each allowing one customer at a time
            safePrint(customerToString() + " wants to enter the restaurant");
            doorSemaphore.acquire();
            safePrint(customerToString() + " enters the restaurant");
            doorSemaphore.release();
        }
        public void exitRestaurant() throws InterruptedException {//controls two doors, each allowing one customer at a time
            doorSemaphore.acquire();
            safePrint(customerToString() + " is leaving the restaurant");
            doorSemaphore.release();
            safePrint(customerToString() + " has left the restaurant");
            customerCounter--;
        }
        public void waitersLeave() throws InterruptedException {
            for (int i = 0; i < waiter.length; i++) {
                    doorSemaphore.acquire();
                    safePrint("Waiter " + waiter[i].waiterId + " cleans the table and leaves the restaurant");
                    doorSemaphore.release();
            }
        }
        public static void safePrint(String line){ //thread safe printing
            synchronized (printlock){
                System.out.println(line);
            }
        }
    }
}
