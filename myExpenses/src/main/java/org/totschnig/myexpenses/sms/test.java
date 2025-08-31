public class test {

    // Example: Creating and saving a transaction
// filepath: your/custom/Class.java

// ...existing code...
Transaction transaction = new Transaction();
transaction.setAccountId(123L); // set your account ID
transaction.setAmount(new Money(currencyUnit, 10000L)); // set amount (minor units)
transaction.setCategoryPath("Food:Supermarket");
transaction.setPayee("Aldi");
transaction.setComment("Groceries shopping");

// Save the transaction
Uri resultUri = transaction.save(context.getContentResolver());
// ...existing code...
}
