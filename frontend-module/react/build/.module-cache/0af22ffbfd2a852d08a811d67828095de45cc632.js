/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {items:[0,1,2,3], count : 0};
  },
  handleClick: function(event) {
      console.log('count');
      this.setState({count: this.state.count+1});
  },
  addItem: function(){
      var temp = this.state.items.push(this.state.items.length);
      this.setState({items: this.state.items});
  },
  render: function() {
    console.log('render LikeButton')
    var text = this.state.liked ? 'like' : 'unlike';
    var self = this;
    return (
      React.DOM.div({style: this.style}, 
        React.DOM.button({onClick: this.addItem}, "Add item"), 
         this.state.items.map(function(l){
            return Cmp({key: l, val: "link"+(l+self.state.count), onClick: self.handleClick})
}) 
)
    );
  }
});

var DummyWrapper = React.createClass({displayName: 'W',
    render: function() {
        console.log('render DummyWrapper');
        var temp = LikeButton(null);
        console.log(temp);
        return temp;
    }
});


var Cmp = React.createClass({displayName: 'Cmp',
    handleClick: function(event) {
        console.log('count');
    },
  render: function() {
    console.log('render Cmp');
    return React.DOM.p({onClick: this.handleClick}, " ", this.props.val, " ");
  }
});

React.renderComponent(
  DummyWrapper(null),
  document.getElementById('example')
);