**Steps to reproduce the issue:**

- Checkout the code and build using maven.

- Start the application on 2 nodes (A & B).

- Use following command to start the application 
        
        java -jar -Dhosts="1.1.1.1,2.2.2.2" ignite-rebalancing-issue-1.0-SNAPSHOT-fat.jar
        
    Here "1.1.1.1,2.2.2.2" is comma separated IP for node A & B
    
- Now look at the logs on youngest node in cluster (suppose node B)
    
    Node B will print user's first name and last name in the logs. This data in put into cache by Node A.
    
- Now bring down the node A. You will see in the logs of Node B that one of data element from cache is gone. 
  
- Logic to reproduce the problem is written in ProblemProducer class. Ignite configuration is done in IgniteCacheConfig class.
