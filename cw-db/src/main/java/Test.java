import edu.uob.Handler;

public class Test {
    private String generateRandomName() {
        String randomName = "";
        for(int i=0; i<10 ;i++) randomName += (char)( 97 + (Math.random() * 25.0));
        return randomName;
    }
    public static void main(String[] args){

        /*Table table = new Table();

        table.readTableFromFile(Paths.get("databases").toAbsolutePath().toString(), "sheds.tab");

        System.out.println("Column Names:" + table.getColumnNames());
        System.out.println("Column Types:" + table.getColumnTypes());
        System.out.println("Rows:");
        for (Map<String, Object> row : table.getRows()){
            System.out.println(row);
        }*/

        String query = "INSERT INTO marks VALUES ('Simon', 65, TRUE);";
        Handler test = new Handler();
        test.setQuery(query);
    }
}
