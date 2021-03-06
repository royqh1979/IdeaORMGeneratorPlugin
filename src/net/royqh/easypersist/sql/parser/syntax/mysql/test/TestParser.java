package net.royqh.easypersist.sql.parser.syntax.mysql.test;

import net.royqh.easypersist.sql.parser.syntax.mysql.MySQLLexer;
import net.royqh.easypersist.sql.parser.syntax.mysql.MySQLParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Created by Roy on 2017/1/14.
 */
public class TestParser {
    public static void main(String[] args) throws IOException {
        // create a CharStream that reads from standard input
        String sql="select  *  from  /* hehehe */  ttt;";
        ByteArrayInputStream is=new ByteArrayInputStream(sql.getBytes("utf8"));
        CharStream input = CharStreams.fromStream(is);
// create a lexer that feeds off of input CharStream
        MySQLLexer lexer = new MySQLLexer(input);
// create a buffer of tokens pulled from the lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer);
// create a parser that feeds off the tokens buffer
        MySQLParser parser = new MySQLParser(tokens);
        ParseTree tree = parser.prog(); // begin parsing at init rule
        tree.accept(new TestSQLVistitor(tokens));
    }
}
