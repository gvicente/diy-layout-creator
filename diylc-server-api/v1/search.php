<?php
	
$className="org.diylc.plugins.cloud.model.ProjectEntity";
$criteria=$_REQUEST["criteria"];
$category=str_replace("- ", "", $_REQUEST["category"]);
$page=$_REQUEST["page"];
$username=$_REQUEST["username"];
$itemsPerPage=$_REQUEST["itemsPerPage"];
$sort=$_REQUEST["sort"];
$projectId=$_REQUEST["projectId"];
if(!$page)
	$page=1;
if(!$itemsPerPage)
	$itemsPerPage=10;
if(!$sort)
	$sort="Project Name";

$condition="";

if ($category)
	$condition = $condition." AND LOWER(c.search_name) LIKE LOWER('%".addslashes($category)."%')";
if ($criteria)
	$condition = $condition." AND (LOWER(p.description) LIKE LOWER('%".addslashes($criteria)."%') OR LOWER(p.name) LIKE LOWER('%".addslashes($criteria)."%') OR LOWER(p.keywords) LIKE LOWER('%".addslashes($criteria)."%'))";
if ($username)
	$condition = $condition." AND u.name = \"".addslashes($username)."\"";
if ($projectId)
	$condition = $condition." AND p.project_id=".addslashes($projectId);
	
$limit = " LIMIT ".$itemsPerPage." OFFSET ".(($page-1)*$itemsPerPage);

$orderBy = " ORDER BY ";
if ($sort==="Author")
	$orderBy = $orderBy." u.name, p.name";	
else if ($sort==="Category")
	$orderBy = $orderBy." c.search_name, p.name";
else if ($sort==="Age")
	$orderBy = $orderBy." p.last_update desc";
else
	$orderBy = $orderBy." p.name";

$params="?criteria=".$criteria."&category=".$category."&format=".$format."&sort=".$sort;
	
// Load help class
require_once("properties.php");

// Load properties
$dbProperties = new Properties();
$propertiesFile = fopen("db.properties", "rb");
$dbProperties->load($propertiesFile);

// Connect to the DB
$username=$dbProperties->getProperty("user");
$password=$dbProperties->getProperty("pass");
$database=$dbProperties->getProperty("db");
$mysqli = new mysqli(localhost,$username,$password,$database);

function ip_details($IPaddress) 
{
	$json       = file_get_contents("http://ip-api.com/json/{$IPaddress}");
	$details    = json_decode($json);
	return $details;
}  

$ip = $_SERVER["REMOTE_ADDR"];
$location = ip_details($ip);

$sql = "INSERT INTO diylc_search_history (ip, country, criteria, category, sort, search_time) VALUES (\"".$ip."\", \"".$location->country."\",\"".addslashes($criteria)."\",\"".addslashes($category)."\",\"".$sort."\",NOW())";

$mysqli->query($sql);

//echo $sql;

//if (!$result = $mysqli->query($sql)) {
//	echo "{\"string\":Error:".$mysqli->error."}";
//	exit;
//}

$sql = "
SELECT p.project_id, p.name, p.description, c.search_name AS 'category', c.display_name AS 'category_for_display', u.name AS 'owner', p.keywords, p.last_update, p.view_count, p.download_count, (SELECT COUNT(*) FROM diylc_comment co WHERE co.project_id = p.project_id) comment_count
FROM diylc_project p, diylc_category_view c, diylc_user u 
WHERE p.deleted = 0 AND p.category_id = c.category_id AND p.owner_user_id = u.user_id ".$condition.$orderBy.$limit;

//echo $sql;

if (!$result = $mysqli->query($sql)) {
	echo "{\"string\":Error:".$mysqli->error."}";
	exit;
}

$num = $num = $result->num_rows;
if ($num === 0) {
	echo "{\"list\":\"\"}";
} else {
	// JSON header
	echo "{\"list\":{\"".$className."\":[";

	$i=0;
	while ($row = $result->fetch_assoc()) {
	if ($i>0) {
		echo ",";
	}
	
	$time = strtotime($row["last_update"]);
	$updated = date("Y-m-d", $time);

	
	echo "{";
	echo "\"id\":".$row["project_id"].",";
	echo "\"name\":\"".addslashes($row["name"])."\",";
	echo "\"description\":\"".addslashes($row["description"])."\",";
	echo "\"owner\":\"".addslashes($row["owner"])."\",";
	echo "\"category\":\"".$row["category"]."\",";
	echo "\"categoryForDisplay\":\"".$row["category_for_display"]."\",";
	echo "\"updated\":\"".$updated."\",";		
	echo "\"keywords\":\"".$row["keywords"]."\",";	
	echo "\"thumbnailUrl\":\"http://diy-fever.com/diylc/api/v1/downloadThumbnail.php?id=".$row["project_id"]."\",";
	echo "\"downloadUrl\":\"http://diy-fever.com/diylc/api/v1/downloadProject.php?id=".$row["project_id"]."\",";        
	echo "\"commentCount\":\"".$row["comment_count"]."\",";
	echo "\"viewCount\":\"".$row["view_count"]."\",";
	echo "\"downloadCount\":\"".$row["download_count"]."\"";
	echo "}";
	
	$i++;
}

	echo "]}}";
}

$result->free();
$mysqli->close();
?>