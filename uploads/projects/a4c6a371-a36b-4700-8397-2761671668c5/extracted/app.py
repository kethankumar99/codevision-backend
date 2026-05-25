from flask import Flask
app = Flask(__name__)

@app.route('/')
def home():
    return {'app': 'flask'}

@app.route('/users', methods=['GET','POST'])
def users():
    return [{'id':1,'name':'John'}]
