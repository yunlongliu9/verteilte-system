so we got another way to optimize the data volume of serialisation; 

we can just use byte[] to origanize all our data that must be transferred between server and client; only necessary part, which depends on the design of our system;

for example; our auctionsystem doesn't need to much information from client; just index like auctionname,price;
so no matter how big the object is , we can just use 2 part
1. string(auction name) 
2. price (float/double)
and we use byte to transfer the data;

Furthermore we can use zip / compression algorithm to optimize the datafeld extremely;