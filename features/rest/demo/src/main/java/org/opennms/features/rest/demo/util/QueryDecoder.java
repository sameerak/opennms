package org.opennms.features.rest.demo.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opennms.core.criteria.Alias.JoinType;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.criteria.restrictions.Restriction;
import org.opennms.core.criteria.restrictions.Restrictions;
import org.opennms.netmgt.model.OnmsNode;

public class QueryDecoder {

    public static Criteria CreateCriteria(String fiqlQuery){
        final CriteriaBuilder builder = new CriteriaBuilder(OnmsNode.class);
        builder.alias("snmpInterfaces", "snmpInterface", JoinType.LEFT_JOIN);
        builder.alias("ipInterfaces", "ipInterface", JoinType.LEFT_JOIN);
        builder.alias("categories", "category", JoinType.LEFT_JOIN);

        builder.orderBy("label").asc();
        
        final Criteria crit = builder.toCriteria();
        
        final List<Restriction> restrictions = new ArrayList<Restriction>(crit.getRestrictions());
        Restriction restriction = removeBrackets(fiqlQuery);
        restrictions.add(restriction);
        crit.setRestrictions(restrictions);
        
        return crit;
    }
    
    /**
     * create a Restriction object out of the queries containing brackets
     * 
     * @param bracketedQuery
     * @return
     */
    private static Restriction removeBrackets(String bracketedQuery) {
        int newOpenBracket = bracketedQuery.indexOf("(");
                
        if (newOpenBracket != -1){ //if provided query contains an opening bracket
            
            //data structures to help completing the creation of complex queries
            List<Restriction> componentRestrictions = new ArrayList<Restriction>(); //to store created restrictions which are to be pivoted later
            List<Character> pivotPoints = new ArrayList<Character>(); //to store values of the pivot points
            //if restrictions.count = "n" then pivotPoints.count = "n - 1"
        
            if (newOpenBracket != 0) { //if new opening bracket is not the starting character
                Restriction restriction = removeBrackets(bracketedQuery.substring(0, newOpenBracket - 1));
                Character pivotOperator = bracketedQuery.charAt(newOpenBracket - 1);
                componentRestrictions.add(restriction);
                pivotPoints.add(pivotOperator);
            }
            //process bracketed part after the non bracketed part
            int respectiveClosingBracket = newOpenBracket + extractBracketEndPoint(bracketedQuery.substring(newOpenBracket));
            componentRestrictions.add(removeBrackets(bracketedQuery.substring(newOpenBracket + 1, respectiveClosingBracket)));
            
            if (respectiveClosingBracket != bracketedQuery.length() - 1){ //if query extends after the closing bracket
                pivotPoints.add(bracketedQuery.charAt(respectiveClosingBracket + 1));
                componentRestrictions.add(removeBrackets(bracketedQuery.substring(respectiveClosingBracket + 2)));
            }
            
            return mergeBracketedRestrictions(componentRestrictions, pivotPoints);
        }
        else{
            return createComplexRestriction(bracketedQuery);
        }
    }
    
    /**
     * This method will return the index of the matching closing bracket for the provided string 
     * 
     * @param processing - a string starting with an opening bracket "("
     * @return index of respective closing bracket wrt starting "("
     */
    private static int extractBracketEndPoint(String processingQuery) {
        int bracketCounter = 0; //to get the location of respective closing bracket
        for (int i = 0; i < processingQuery.length(); i++) {//iterate over the given string 
            Character test = processingQuery.charAt(i);
            if (test == '(') {
                bracketCounter++;//for "(" increment bracketCounter
            } else if (test == ')') {
                bracketCounter--;//for ")" decrement bracketCounter
                if (bracketCounter == 0) { 
                    //return respective closing location 
                    return i;
                }
            }
        }
        return 0; //if matching bracket is not found
    }

    /**
     * method to merge restriction components disassembled in the process removing brackets
     * In the merging process AND is given priority over OR
     * 
     * @param componentRestrictions - disassembled Restrictions
     * @param pivotPoints - list of ";" and "," (AND, OR of FIQL)
     * @return
     */
    private static Restriction mergeBracketedRestrictions(
                                                    List<Restriction> componentRestrictions,
                                                    List<Character> pivotPoints) {
        Restriction result = componentRestrictions.get(0);
        
        if (!pivotPoints.isEmpty()){ //If no pivot points exist
            List<Restriction> andMergedList = new ArrayList<Restriction>(); //data structure to help merge using AND operators
            andMergedList.add(result);
            int pivotIndex = 0;
            
            for (Character pivot : pivotPoints) {
                if (pivot == ';') {
                    //if AND pivot point is detected remove last element from andMergeList
                    //create new and-restriction with removed element and next component restriction
                    //add that element to andMergeList
                    result = Restrictions.and(
                                              andMergedList.remove(andMergedList.size() - 1), 
                                              componentRestrictions.get(pivotIndex + 1));
                    andMergedList.add(result);
                } else {
                    //else just add the next component restriction to andMergeList
                    andMergedList.add(componentRestrictions.get(pivotIndex + 1));
                }
                
                pivotIndex++; //increment pivot index to access next restriction element
            }
            
            result = andMergedList.get(0);
            
            if (andMergedList.size() > 1){//if andMergedList has more than 1 element
                //merge all elements with logical OR
                for (int i = 1; i < andMergedList.size(); i++) {
                    result = Restrictions.or(result, andMergedList.get(i));
                }
            }
        }
        
        return result;
    }

    /**
     * Method takes a complex query with AND (;) & OR (,)
     * and returns the matching restriction
     * 
     * @param complexQuery - query containing AND (;) & OR (,)
     * @return
     */
    private static Restriction createComplexRestriction(String complexQuery) {
        if (!complexQuery.contains(";") && !complexQuery.contains(",")) {
            return createPrimitiveRestriction(complexQuery);
        }
        String[] primitiveQueryStrings = complexQuery.split(",|;");
        
        List<Restriction> componentRestrictions = new ArrayList<Restriction>(); //to store created primitive restrictions which are to be pivoted later
        for (String primitiveQuery : primitiveQueryStrings) {
            componentRestrictions.add(createPrimitiveRestriction(primitiveQuery));
        }
        
        List<Character> pivotPoints = new ArrayList<Character>(); //to store values of the pivot points
        for (int i = 0; i < complexQuery.length(); i++) {//iterate over the given complexQuery string 
            Character test = complexQuery.charAt(i);
            if (test == ';' || test == ',') {
                pivotPoints.add(test);
            }
        }
        
        return mergeBracketedRestrictions(componentRestrictions, pivotPoints);
    }

    /**
     * method to create restrictions for the primitive operators
     * primitive operators = (==, !=, =lt=, =le=, =gt=, =ge=)
     * 
     * @param primitiveQuery
     * @return
     */
    private static Restriction createPrimitiveRestriction(
                                            String primitiveQuery) {
        //pre-processing the string by split("=")
        String[] componentStrings = primitiveQuery.split("=");
                
        if (componentStrings.length == 2 && componentStrings[0].endsWith("!")) {//case "!="
            String propertyName = componentStrings[0].substring(0, componentStrings[0].length() - 1);
            Object compareWith = getCompareObject(propertyName, componentStrings[1]);
            return Restrictions.ne(propertyName, compareWith);
        } 
        else if (componentStrings.length == 3) {
            Object compareWith = getCompareObject(componentStrings[0], componentStrings[2]);
            
            if (componentStrings[1].equals("")) {//case "=="
                return Restrictions.eq(componentStrings[0], compareWith); 
            } 
            else if (componentStrings[1].equals("lt")) {//case "=lt="
                return Restrictions.lt(componentStrings[0], compareWith); 
            } 
            else if (componentStrings[1].equals("le")) {//case "=le="
                return Restrictions.le(componentStrings[0], compareWith); 
            } 
            else if (componentStrings[1].equals("gt")) {//case "=gt="
                return Restrictions.gt(componentStrings[0], compareWith); 
            } 
            else if (componentStrings[1].equals("ge")) {//case "=ge="
                return Restrictions.ge(componentStrings[0], compareWith); 
            }
        }
        return null;
    }

    /**
     * For the given property name respective comparable object is created
     * ex - createTime -> java.util.Date
     * 
     * TODO - extend to provide validations
     * 
     * @param propertyName
     * @param compareValue
     * @return
     */
    private static Object getCompareObject(String propertyName, String compareValue) {
        if (propertyName.equals("createTime") || propertyName.equals("lastCapsdPoll")) {
            DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                Date formattedDate = formatter.parse(compareValue);
                return formattedDate;
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return compareValue;
    }

}
