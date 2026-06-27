const mysql = require('mysql2');
const pool = mysql.createPool({
  host: 'localhost',
  user: 'lit_user',
  password: '123456',
  database: 'literature_db',
  waitForConnections: true,
  connectionLimit: 10,
  queueLimit: 0
});
module.exports = pool.promise();