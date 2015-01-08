/** @jsx React.DOM */
var LikeButton = React.createClass({displayName: 'LikeButton',
  getInitialState: function() {
    return {items:[0,1,2,3]};
  },
  handleClick: function(event) {

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
            return Cmp({key: l, val: "test", onClick: self.handleClick}, "link", l)
        }) 
      )
    );
  }
});

var DummyWrapper = React.createClass({displayName: 'W',
    render: function() {
        console.log('render DummyWrapper');
        return React.DOM.p(null, " ", this.props.val, " ");
    }
});


var Cmp = React.createClass({displayName: 'Cmp',
  render: function() {
    console.log('render Cmp');
    return React.DOM.p(null, " ", this.props.val, " ");
  }
});

React.renderComponent(
  LikeButton(null),
  document.getElementById('example')
);