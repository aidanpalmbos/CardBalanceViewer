var express = require('express');

var mysql = require('mysql');

var app = express();
var port = 3309;
app.use(express.json());

var apiKey = "palmbos0103palmbos0103";

var con = mysql.createConnection({
    host: "127.0.0.1",
    user: "test",
    password: "02162004",
    dateStrings: 'date',
});

function mysqlMakeConection() {
    var con = mysql.createConnection({
        host: "127.0.0.1",
        user: "test",
        password: "02162004",
        dateStrings: 'date',
    });
    con.connect(function (err) {
        if (err) throw err;
        console.log("Made Connection");
    });

}

//Load all cards from database:
app.post("/loadCards/:apiKey", (req, res) => {
    if (req.params.apiKey != apiKey) {
        res.send(false);
        return;
    }
    if (con.state === 'disconnected') {
        mysqlMakeConection();
    }

    con.query("SELECT * FROM cardbase.cards;", function (err, results) {
        if (err) {
            console.log(err);
            res.send(false);
        }
        else {
            let sendBack = "";
            for (var result of results) {
                sendBack += `${result.name}|${result.balance}|${result.date}|`;
            }
            res.send(sendBack);
        }
    });
});
//Save changed data:
app.post("/saveCard/:apiKey/:cardName/:balance/:originalName", (req, res) => {
    if (req.params.apiKey != apiKey) {
        res.send(false);
        return;
    }
    if (con.state === 'disconnected') {
        mysqlMakeConection();
    }

    let cardName = mysql.escape(req.params.cardName);
    let balance = mysql.escape(req.params.balance);
    let date = new Date();
    date.setHours(date.getHours() - 4);
    let dateString = date.toISOString().slice(0, 19).replace('T', ' '); //Set time to home of server
    let originalName = mysql.escape(req.params.originalName);

    let queryString = `UPDATE cardbase.cards SET name = ${cardName}, balance = ${balance}, date = '${dateString}' WHERE (name = ${originalName});`;

    con.query(queryString, function (err, results) {
        if (err) {
            console.log(err);
            res.send(false);
        }
        else {
            res.send(true);
        }
    });
});
//Create a new card in database:
app.post("/createCard/:apiKey", (req, res) => {
    if (req.params.apiKey != apiKey) {
        res.send(false);
        return;
    }
    if (con.state === 'disconnected') {
        mysqlMakeConection();
    }

    let randomNumber = Math.floor(Math.random() * 99999);
    let queryString = `INSERT INTO cardbase.cards (name) VALUES ('card${randomNumber}')`;

    con.query(queryString, function (err, results) {
        if (err) {
            console.log(err);
            res.send(false);
        }
        else {
            res.send(`card${randomNumber}`);
        }
    });
});
//Test connection to server
app.get("/testConnection", (req, res) => {
    res.send("Connection recieved");
});

app.listen(port, () => {
    console.log("Server Started");
    mysqlMakeConection();
});